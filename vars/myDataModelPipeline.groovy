def call(body) {
    def pipelineParameters = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParameters
    body()

    pipeline {
        agent any
        stages {
            stage('build') {
                steps {
                    echo "Building"
                }
            }

            stage('test') {
                steps {
                    echo "Testing $pipelineParameters.name"
                }
            }
        }
    }
}
