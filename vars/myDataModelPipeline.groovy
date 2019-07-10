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
                    getcause()
                }
            }
            stage('lint') {
                when {
                    anyof {
                        environment name: 'build_cause', value: 'manualtrigger'
                        changeset "${env.project_name}/**"
                    }
                }
                steps {
                    githubstatus('pending', "${env.project_name}/${env.stage_name}")
                    dir("${env.project_name}") {
                        sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml down -v'
                        sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml run app'
                    }
                    githubstatus('success', "${env.project_name}/${env.stage_name}")
                }
                post {
                    failure {
                        githubstatus('failure', "${env.project_name}/${env.stage_name}")
                    }
                    always {
                        dir("${env.project_name}") {
                            sh 'docker-compose -f docker-compose.yml -f docker-compose.jenkins.yml down -v'
                        }
                    }
                }
            }
            stage('build') {
                when {
                    anyof {
                        environment name: 'build_cause', value: 'manualtrigger'
                        changeset "${env.project_name}/**"
                    }
                }
                steps {
                    dir("${env.project_name}") {
                        withdockercontainer(image: "docker.tntdigital.io/tnt/data/data-model-builder:master") {
                            sh "build-artifact"
                        }
                        sh "echo ${env.git_commit_short} > build/version.txt"
                    }
                }
            }
            stage("deploy-sandbox") {
                when {
                    allof {
                        expression { return params.deploy_sandbox }
                        anyof {
                            environment name: 'build_cause', value: 'manualtrigger'
                            changeset "${env.project_name}/**"
                        }
                    }
                }
                steps {
                    dir("${env.project_name}") {
                        githubstatus('pending', "${env.stage_name}-${env.project_name}")

                        withdockercontainer(image: "docker.tntdigital.io/tnt/data-aws-cli:master", args: '--entrypoint ""') {
                            sh "/usr/bin/echo deploy code here"
                        }

                        githubstatus('success', "${env.stage_name}-${env.project_name}")
                    }
                }
                post {
                    failure {
                        githubstatus('failure', "${env.stage_name}-${env.project_name}")
                    }
                }
            }
            stage("deploy-dev") {
                when {
                    allof {
                        expression { return params.deploy_dev }
                        anyof {
                            environment name: 'build_cause', value: 'manualtrigger'
                            changeset "${env.project_name}/**"
                        }
                    }
                }
                steps {
                    dir("${env.project_name}") {
                        githubstatus('pending', "${env.stage_name}-${env.project_name}")

                        withdockercontainer(image: "docker.tntdigital.io/tnt/data-aws-cli:master", args: '--entrypoint ""') {
                            sh "/usr/bin/echo deploy code here"
                        }

                        githubstatus('success', "${env.stage_name}-${env.project_name}")
                    }
                }
                post {
                    failure {
                        githubstatus('failure', "${env.stage_name}-${env.project_name}")
                    }
                }
            }
            stage("deploy-prd") {
                when {
                    allof {
                        branch 'master'
                        expression { return params.deploy_prd }
                        anyof {
                            environment name: 'build_cause', value: 'manualtrigger'
                            changeset "${env.project_name}/**"
                        }
                    }
                }
                steps {
                    dir("${env.project_name}") {
                        githubstatus('pending', "${env.stage_name}-${env.project_name}")

                        withdockercontainer(image: "docker.tntdigital.io/tnt/data-aws-cli:master", args: '--entrypoint ""') {
                            sh "/usr/bin/echo deploy code here"
                        }

                        githubstatus('success', "${env.stage_name}-${env.project_name}")
                    }
                }
                post {
                    failure {
                        githubstatus('failure', "${env.stage_name}-${env.project_name}")
                    }
                }
            }
        }
        post {
            failure {
                script {
                    if( "${env.git_branch}" == 'master' ) {
                        slacksend channel:''
                    }
                }
            }
        }
    }
}
