pipeline {
    agent any

    stages {
        stage('mobilize deploy file') {
            steps {
                sshPublisher(publishers: [sshPublisherDesc(configName: 'ecs_czystudy', transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: '/czystudy/deploy.sh', execTimeout: 120000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)])
            }
        }
    }
}