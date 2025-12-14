import jenkins.model.*
import org.jenkinsci.plugins.github_branch_source.*
import jenkins.branch.*
import org.jenkinsci.plugins.workflow.multibranch.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition

def env = System.getenv()
def instance = Jenkins.get()

def jobName       = "bankapp"
def kafkaJobName  = "bankapp-kafka-deploy"
def githubRepo    = env['GITHUB_REPOSITORY']
def credentialsId = "github-creds"
def mainScriptPath = "Jenkinsfile"
def kafkaScriptPath = "Jenkinsfile-kafka"

println "--> Запуск create-multibranch-job.groovy"

if (!githubRepo) {
    println "Переменная окружения GITHUB_REPOSITORY не задана (пример: owner/repo)"
    return
}

println "--> GITHUB_REPOSITORY = ${githubRepo}"

// --- Создание Multibranch Pipeline для основного приложения ---
if (instance.getItem(jobName) != null) {
    println "--> Multibranch job '${jobName}' уже существует. Пропускаем."
} else {
    // (Код для создания multibranch job остается без изменений)
    def parts = githubRepo.split('/')
    if (parts.length != 2) {
        println "Неверный формат GITHUB_REPOSITORY. Ожидалось: owner/repo"
        return
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
    mbp.scheduleBuild2(0)

    println "--> Multibranch job '${jobName}' создан и запущен на '${githubRepo}'"
}

// --- Создание Single Pipeline Job для Kafka ---
if (instance.getItem(kafkaJobName) != null) {
    println "--> Pipeline job '${kafkaJobName}' уже существует. Пропускаем."
} else {
    // Создаём обычную Pipeline задачу
    WorkflowJob kafkaJob = instance.createProject(WorkflowJob.class, kafkaJobName)

    // Настройка источника скрипта: Jenkinsfile-kafka из SCM
    def definition = new CpsScmFlowDefinition(
            new org.jenkinsci.plugins.workflow.steps.scm.GitStep(
                    [$class: 'GitStep',
                     remote: "https://github.com/${githubRepo}.git", // Используем тот же репозиторий
                     credentialsId: credentialsId]
            ).getSCM(),
            kafkaScriptPath // Указываем путь к Jenkinsfile-kafka
    )
    kafkaJob.setDefinition(definition)

    // Сохраняем и выводим сообщение
    kafkaJob.save()
    println "--> Pipeline job '${kafkaJobName}' создан. Будет использовать '${kafkaScriptPath}' из репозитория."

    // Опционально: Запустить джоб сразу для проверки
    // kafkaJob.scheduleBuild2(0)
}
