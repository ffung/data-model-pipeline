def call(body) {
    def pipelineParameters = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParameters
    body()

    pipeline {
        agent any
        stages {
            stage('check-changes') {
                steps {
                    getCause()
                }
            }
            stage('lint') {
                when {
                    anyOf {
                        environment name: 'BUILD_CAUSE', value: 'MANUALTRIGGER'
                        changeset "${env.PROJECT_NAME}/**"
                    }
                }
                steps {
                    githubStatus('pending', "${env.PROJECT_NAME}/${env.STAGE_NAME}")
                    dir("${env.PROJECT_NAME}") {
                        sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml down -v'
                        sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml run app'
                    }
                    githubStatus('success', "${env.PROJECT_NAME}/${env.STAGE_NAME}")
                }
                post {
                    failure {
                        githubStatus('failure', "${env.PROJECT_NAME}/${env.STAGE_NAME}")
                    }
                    always {
                        dir("${env.PROJECT_NAME}") {
                            sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml down -v'
                        }
                    }
                }
            }
            stage('build') {
                when {
                    anyOf {
                        environment name: 'BUILD_CAUSE', value: 'MANUALTRIGGER'
                        changeset "${env.PROJECT_NAME}/**"
                    }
                }
                steps {
                    dir("${env.PROJECT_NAME}") {
                        sh "echo ${env.GIT_COMMIT_SHORT} > build/version.txt"
                    }
                }
            }
            stage("deploy-sandbox") {
                when {
                    allOf {
                        expression { return params.DEPLOY_SANDBOX }
                        anyOf {
                            environment name: 'BUILD_CAUSE', value: 'MANUALTRIGGER'
                            changeset "${env.PROJECT_NAME}/**"
                        }
                    }
                }
                steps {
                    dir("${env.PROJECT_NAME}") {
                        githubStatus('pending', "${env.STAGE_NAME}-${env.PROJECT_NAME}")

                        echo "do something useful"

                        githubStatus('success', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                    }
                }
                post {
                    failure {
                        githubStatus('failure', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                    }
                }
            }
            stage("deploy-dev") {
                when {
                    allOf {
                        expression { return params.DEPLOY_DEV }
                        anyOf {
                            environment name: 'BUILD_CAUSE', value: 'MANUALTRIGGER'
                            changeset "${env.PROJECT_NAME}/**"
                        }
                    }
                }
                steps {
                    dir("${env.PROJECT_NAME}") {
                        githubStatus('pending', "${env.STAGE_NAME}-${env.PROJECT_NAME}")

                        echo "do something useful"

                        githubStatus('success', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                    }
                }
                post {
                    failure {
                        githubStatus('failure', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                    }
                }
            }
            stage("deploy-prd") {
                when {
                    allOf {
                        branch 'master'
                        expression { return params.DEPLOY_PRD }
                        anyOf {
                            environment name: 'BUILD_CAUSE', value: 'MANUALTRIGGER'
                            changeset "${env.PROJECT_NAME}/**"
                        }
                    }
                }
                steps {
                    dir("${env.PROJECT_NAME}") {
                        githubStatus('pending', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                    }

                    echo "do something useful"

                    githubStatus('success', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                }
            }
            post {
                failure {
                    githubStatus('failure', "${env.STAGE_NAME}-${env.PROJECT_NAME}")
                }
            }
        }
    }
    post {
        failure {
            script {
                if( "${env.GIT_BRANCH}" == 'master' ) {
                    slackSend channel:''
                }
            }
        }
    }
}
