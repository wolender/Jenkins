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
        stage('Pull') {
            steps {
                // Get some code from a GitHub repository
                git branch: 'dev', url:'https://github.com/wolender/FinalProject.git'
            }
        }
        stage('Build') {
            steps{
               sh 'mvn clean install -DskipTests'
            }
        }
        stage('Test'){
            steps{

                sh 'mvn test'
                

                junit 'target/surefire-reports/*.xml'
            }
        }
        stage ('Deploy to nexus'){
            steps{
                sh 'mvn -s $SETTINGS deploy -DskipTests'
            }
        }
        stage ('Docker build'){
            steps{
                sh 'docker build -t petclinic .'
            }
        }
        stage ('Push to dockerHub'){
            steps{
                sh 'docker login'
                sh 'docker tag petclinic wolender/snapshot_repo'
                sh 'docker push wolender/snapshot_repo'
            }
        }
    }
}
