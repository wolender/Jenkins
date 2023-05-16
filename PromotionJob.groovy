pipeline {
    agent any
    tools {
        // Install the Maven version configured as "M3" and add it to the path.
        maven "maven"
    }
    environment {
        SETTINGS = '/var/lib/jenkins/settings.xml'
    }
    stages {
        stage('Get Artifact') {
            steps {
                sh 'mvn dependency:copy -Dartifact=org.springframework.samples:spring-petclinic:$ARTIFACT_VERSION -DoutputDirectory=.  -s $SETTINGS -e -X -U'
            }
        }
        stage('Install') {
            steps {
                sh 'mvn install:install-file -Dfile=./spring-petclinic-$ARTIFACT_VERSION.jar -Dversion=$RELEASE_VERSION -Dskip_tests -DlocalRepositoryPath=.'
            }
        }
        stage ('Change version') {
            steps {
                
                sh 'mvn -f org/springframework/samples/spring-petclinic/$RELEASE_VERSION/spring-petclinic-$RELEASE_VERSION.pom versions:set -DnewVersion=$RELEASE_VERSION'
            }
        }
        stage('Upload to nexus') {
            steps {
                sh 'mvn deploy:deploy-file -s $SETTINGS -DgroupId=org.springframework.samples -DartifactId=spring-petclinic -Dversion=$RELEASE_VERSION -Dpackaging=jar -Dfile=org/springframework/samples/spring-petclinic/$RELEASE_VERSION/spring-petclinic-$RELEASE_VERSION.jar -DrepositoryId=local-nexus -Durl="http://$NEXUS_SERVER:8081/repository/maven-releases/" -e -X'
            }
        }
        stage('pull from nexus') {
            steps {
                sh 'mvn dependency:copy -Dartifact=org.springframework.samples:spring-petclinic:$RELEASE_VERSION -DoutputDirectory=.  -s $SETTINGS -e -X -U'
            }
        }
        stage('Build Image') {
            steps {
                git branch: 'main', url:'https://github.com/wolender/dockerfile.git'
                sh 'docker build --build-arg JAR_VERSION=$RELEASE_VERSION -t petclinic:$RELEASE_VERSION .'
            }
        }
        stage ('Push to dockerHub'){
            steps{
                sh 'docker login'
                sh 'docker tag petclinic:$RELEASE_VERSION wolender/release_repo:$RELEASE_VERSION'
                sh 'docker push wolender/release_repo:$RELEASE_VERSION'
            }
        }
        stage ('Trigger Deploy'){
            steps{
                script{
                    def jobName = 'DeployJob'
                    def parameters = [
                        [$class: 'StringParameterValue', name: 'VERSION', value: env.RELEASE_VERSION]
                    ]
                    build job: jobName, parameters: parameters
                }
            }
        }
    }
}
