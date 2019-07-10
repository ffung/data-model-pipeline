def call(body) {
    def pipelineParameters [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParameters
    body()

    pipeline {
        stages {
            stage('build') {
                steps {
                    echo "Building"
                }
            }

            stage('test') {
                steps {
                    echo "Testing $pipelineParams.name"
                }
            }
        }
    }
}
