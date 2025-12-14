import jenkins.model.*
import org.jenkinsci.plugins.github_branch_source.*
import jenkins.branch.*
import org.jenkinsci.plugins.workflow.multibranch.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition

def env = System.getenv()
def instance = Jenkins.get()

def jobName       = "bankapp" // Имя вашей multibranch джобы
def kafkaJobName  = "bankapp-kafka-deploy" // Имя вашей новой джобы для Kafka
def githubRepo    = env['GITHUB_REPOSITORY'] // Убедитесь, что это задано в .env
def credentialsId = "github-creds" // Убедитесь, что credential существует
def mainScriptPath = "Jenkinsfile"          // Путь к вашему основному Jenkinsfile
def kafkaScriptPath = "jenkins/Jenkinsfile-kafka"   // Путь к вашему Jenkinsfile-kafka в репозитории

println "--> Запуск 02_create-multibranch-job.groovy"

if (!githubRepo) {
    println "ERROR: Переменная окружения GITHUB_REPOSITORY не задана (пример: owner/repo). Проверьте файл .env."
    // Не возвращаемся, чтобы не мешать созданию Kafka job, если multibranch зависит от этого
    // Но если multibranch не нужен без GITHUB_REPOSITORY, можно return
} else {
    println "--> GITHUB_REPOSITORY = ${githubRepo}"

    // --- Создание Multibranch Pipeline для основного приложения (только если GITHUB_REPOSITORY задан) ---
    if (instance.getItem(jobName) != null) {
        println "--> Multibranch job '${jobName}' уже существует. Пропускаем."
    } else {
        if (!githubRepo) {
            println "WARNING: GITHUB_REPOSITORY не задан, пропускаю создание '${jobName}'."
        } else {
            // (Код для создания multibranch job остается без изменений)
            def parts = githubRepo.split('/')
            if (parts.length != 2) {
                println "ERROR: Неверный формат GITHUB_REPOSITORY. Ожидалось: owner/repo"
                return // Если multibranch критична и формат неверен, можно выйти
            }
            def owner = parts[0]
            def repo  = parts[1]

            def source = new GitHubSCMSource(owner, repo)
            source.setCredentialsId(credentialsId)
            source.setTraits([
                    new BranchDiscoveryTrait(1),
                    new OriginPullRequestDiscoveryTrait(1),
                    new ForkPullRequestDiscoveryTrait(1, new ForkPullRequestDiscoveryTrait.TrustPermission())
            ])

            def branchSource = new BranchSource(source)
            branchSource.setStrategy(new DefaultBranchPropertyStrategy([] as BranchProperty[]))

            def mbp = new WorkflowMultiBranchProject(instance, jobName)
            mbp.getSourcesList().add(branchSource)

            def factory = new WorkflowBranchProjectFactory()
            factory.setScriptPath(mainScriptPath)
            mbp.setProjectFactory(factory)

            instance.add(mbp, jobName)
            mbp.save()
            mbp.scheduleBuild2(0) // Опционально запустить сразу

            println "--> Multibranch job '${jobName}' создан и запущен на '${githubRepo}'"
        }
    }
} // Конец условия if (!githubRepo) для multibranch


// --- Создание Single Pipeline Job для Kafka (всегда пытаемся создать, если GITHUB_REPOSITORY задан) ---
// NOTE: Мы также можем сделать создание Kafka job зависимым от GITHUB_REPOSITORY,
// так как она берет Jenkinsfile из SCM. Если GITHUB_REPOSITORY не задан, создание невозможно.
if (!githubRepo) {
    println "WARNING: GITHUB_REPOSITORY не задан, пропускаю создание '${kafkaJobName}' (требуется для доступа к Jenkinsfile-kafka)."
} else {
    if (instance.getItem(kafkaJobName) != null) {
        println "--> Pipeline job '${kafkaJobName}' уже существует. Пропускаем."
    } else {
        try {
            // Создаём обычную Pipeline задачу
            WorkflowJob kafkaJob = instance.createProject(WorkflowJob.class, kafkaJobName)

            // Настройка источника скрипта: Jenkinsfile-kafka из SCM
            // ВАЖНО: Убедитесь, что 'credentialsId' существует в Jenkins
            def scm = new hudson.plugins.git.GitSCM(
                    [new hudson.plugins.git.UserRemoteConfig("https://github.com/${githubRepo}.git", credentialsId, '', '')],
                    [new hudson.plugins.git.BranchSpec("*/main")], // Или master, или другая default ветка
                    false, [], null, null, []
            )

            def definition = new CpsScmFlowDefinition(scm, kafkaScriptPath) // Используем Jenkinsfile-kafka
            kafkaJob.setDefinition(definition)

            // Сохраняем и выводим сообщение
            kafkaJob.save()
            println "--> Pipeline job '${kafkaJobName}' создан. Использует '${kafkaScriptPath}' из репозитория '${githubRepo}'."

            // Опционально: Запустить джоб сразу для проверки
            // kafkaJob.scheduleBuild2(0)

        } catch (Exception e) {
            println "ERROR: Не удалось создать джобу '${kafkaJobName}': ${e.getMessage()}"
            e.printStackTrace() // Вывести stack trace в лог
        }
    }
}
