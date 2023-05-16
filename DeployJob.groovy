pipeline {
    agent {
        label 'agent1'
    }

    stages {
        stage('Docker Login') {
            steps {
                script{
                    withCredentials([usernamePassword(credentialsId: 'sql_pass', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                      // Find the container ID of the container running on port 80/3306
                      def old_container_id = sh(returnStdout: true, script: "docker ps -q --filter \"publish=80\"").trim()
                      def database_id = sh(returnStdout: true, script: "docker ps -q --filter \"publish=3306\"").trim()
                    
                      // Stop the old container
                      if (old_container_id) {
                        sh "docker stop $old_container_id"
                      }
                      // Start db if not started
                      if (!database_id) {
                        sh 'docker run --network=my-network -e MYSQL_USER=$USERNAME -e MYSQL_PASSWORD=$PASSWORD -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=petclinic -p 3306:3306 -d mysql:8.0'
                      }
                      sh 'docker login'
                      sh 'docker pull wolender/release_repo:$VERSION'
                      
                      sh 'docker run -p 80:8080 --network=my-network -e DB_MODE=$DB_MODE -e MYSQL_URL=jdbc:mysql://$SQL_URL:3306/petclinic -d wolender/release_repo:$VERSION '
                    }
                }
                
            }
        }
    }
}
