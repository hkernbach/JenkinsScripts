#!groovy
// We need these modules:
//
// We need permisions for several string manipulation operations, like take()
def REGISTRY="192.168.0.1"
def REGISTRY_URL="https://${REGISTRY}/"
def DOCKER_CONTAINER="ubuntusixteenofour"
def OS="Linux"
def RELEASE_OUT_DIR="/net/fileserver/"
def LOCAL_TAR_DIR="/mnt/workspace/tmp/"
def branches = [:]
def failures = ""
def paralellJobNames = []
def ADMIN_ACCOUNT = "willi@arangodb.com"
def lastKnownGoodGitFile="${RELEASE_OUT_DIR}/${env.JOB_NAME}.githash"
def lastKnownGitRev=""
def currentGitRev=""

def BUILT_FILE = ""
def DIST_FILE = ""
def fatalError = false

def foo = [
  "bar" : [ 1, 3, 5],
  "blub" : [ "z": [5, 7]]
];

def setDirectories(where, String localTarDir, String OS, String jobName, String MD5SUM) {
   localTarball="${localTarDir}/arangodb-${OS}.tar.gz"
   where['localTarball'] = localTarball
   where['localWSDir']="${localTarDir}/${jobName}"
   where['localExtractDir']=where['localWSDir'] + "/x/"
   where['MD5SUM'] = MD5SUM
}


def copyExtractTarBall (where) {
  print("xxx" + where['localTarball']+ "yyy\n")
//
//    sh """
//if test ! -d ${myLocalTarDir}; then
//        mkdir -p ${myLocalTarDir}
//fi
//if test ! -d ${localWSDir}; then
//        mkdir -p ${localWSDir}
//fi
//if test ! -d ${localExtractDir}; then
//        mkdir -p ${localExtractDir}
//fi
//python /usr/bin/copyFileLockedIfNewer.py ${myMD5SUM} ${myDIST_FILE} ${mylocalWSDir} ${localTarball} 'rm -rf ${localExtractDir}; mkdir ${localExtractDir}; cd ${localExtractDir}; tar -xzf ${localTarball}'
//"""
}
//
//def setupTestArea(where) {
//  sh "rm -rf ${testWorkingDirectory}/out/*"
//  sh "find -type l -exec rm -f {} \\; ; ln -s ${localExtractDir}/* ${testWorkingDirectory}/"
// }
//def Boolean runTests (where) {
//  def EXECUTE_TEST="""pwd;
//         TMPDIR=${testWorkingDirectory}/out/tmp
//         mkdir -p \${TMPDIR}
//         echo 0 > ${testWorkingDirectory}/out/rc
//         `pwd`/scripts/unittest ${unitTests} \
//                --skipNondeterministic true \
//                --skipTimeCritical true \
//                ${cmdLineArgs} || \
//         echo \$? > ${testWorkingDirectory}/out/rc"""
//  echo "${unitTests}: ${EXECUTE_TEST}"
//  sh "${EXECUTE_TEST}"
//  shellRC = readFile('${testWorkingDirectory}/out/rc').trim()
//  if (shellRC != "0") {
//    echo "SHELL EXITED WITH FAILURE: ${shellRC}xxx"
//    failures = "${failures}\n\n test ${testRunName} exited with ${shellRC}"
//    currentBuild.result = 'FAILURE'
//  }
//  echo "${unitTests}: recording results"
//  junit '${failureOutput}/out/UNITTEST_RESULT_*.xml'
//  failureOutput=readFile('${testWorkingDirectory}/out/testfailures.txt')
//  if (failureOutput.size() > 5) {
//    failures = "${failureOutput}"
//    return false;
//  }
//  return true;
//}

echo "bla"
stage("cloning source")
  node {
    echo "new foo: "
    print(foo)
    echo "haha!1"
    print(foo["bar"])
    echo "haha!2"
    z = foo["blub"]
    echo "haha!3"
    z["z"] = [987, 345]
    echo "haha!4"
    print(z)
    setDirectories(z, "/somewhere", "linux", 'sanoteuh', "xxx")
    
    echo "haha!5"
    print(foo)
    echo "haha!6"
    copyExtractTarBall(z)
    sh "mount"
    sh "pwd"
    sh "ls -l /jenkins/workspace"
    sh "cat /etc/issue /jenkins/workspace/issue"
    def someString="1234567890"
    echo someString.take(5)
    
    if (fileExists(lastKnownGoodGitFile)) {
      lastKnownGitRev=readFile(lastKnownGoodGitFile)
    }
    git url: 'https://github.com/arangodb/arangodb.git', branch: 'devel'
    currentGitRev = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    print("GIT_AUTHOR_EMAIL: ${env} ${currentGitRev}")
  }

stage("building ArangoDB")
try {
  node {
    OUT_DIR = ""
    docker.withRegistry(REGISTRY_URL, '') {
      def myBuildImage=docker.image("${DOCKER_CONTAINER}/build")
      myBuildImage.pull()
      docker.image(myBuildImage.imageName()).inside('--volume /mnt/data/fileserver:/net/fileserver:rw --volume /jenkins:/mnt/:rw ') {
        sh "mount"
        sh "pwd"
        sh "cat /etc/issue /mnt/workspace/issue"

        sh 'pwd > workspace.loc'
        WORKSPACE = readFile('workspace.loc').trim()
        OUT_DIR = "${WORKSPACE}/out"

        try {
          sh "./Installation/Jenkins/build.sh standard  --rpath --parallel 5 --buildDir build-package-${DOCKER_CONTAINER} --jemalloc --targetDir ${OUT_DIR} "
        } catch (err) {
          stage('Send Notification for failed build' ) {
            gitCommitter = sh(returnStdout: true, script: 'git --no-pager show -s --format="%ae"')

            mail (to: gitCommitter,
                  subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) 'building ArangoDB' failed to compile.", 
                  body: err.getMessage());
            currentBuild.result = 'FAILURE'
            throw(err)
          }
        }
        //sh "./Installation/Jenkins/build.sh standard  --rpath --parallel 5 --package RPM --buildDir build-package --jemalloc --targetDir ${OUT_DIR} "
        BUILT_FILE = "${OUT_DIR}/arangodb-${OS}.tar.gz"
        DIST_FILE = "${RELEASE_OUT_DIR}/arangodb-${OS}.tar.gz"
        MD5SUM = readFile("${BUILT_FILE}.md5").trim()
        echo "copying result files: '${MD5SUM}' '${BUILT_FILE}' '${DIST_FILE}.lock' '${DIST_FILE}'"

        sh "python /usr/bin/copyFileLockedIfNewer.py ${MD5SUM} ${BUILT_FILE} ${DIST_FILE}.lock ${DIST_FILE} "

        sh "ls -l ${RELEASE_OUT_DIR}"
      }
    }
  }
} catch (err) {
    stage('Send Notification for build' )
    mail (to: ADMIN_ACCOUNT, 
          subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) 'building ArangoDB' has had a FATAL error.", 
          body: err.getMessage());
    currentBuild.result = 'FAILURE'
    throw(err)
}

stage("running unittest")
try {
  def testCaseSets = [ 
    //  ["fail", 'fail', ""],
    //    ["fail", 'fail', ""],
    ['ssl_server', 'ssl_server', ""], // FC: don't need this with clusters.
    
//    ['http_server', 'http_server', "",
//     "--cluster true --testBuckets 4/1 ",
//     "--cluster true --testBuckets 4/2 ",
//     "--cluster true --testBuckets 4/3 ",
//     "--cluster true --testBuckets 4/4 "],
//    ["shell_client", 'shell_client', "",
//     "--cluster true --testBuckets 4/1 ",
//     "--cluster true --testBuckets 4/2 ",
//     "--cluster true --testBuckets 4/3 ",
//     "--cluster true --testBuckets 4/4 "],
//    ["shell_server_aql", 'shell_server_aql', "",
//     "--cluster true --testBuckets 4/1 ",
//     "--cluster true --testBuckets 4/2 ",
//     "--cluster true --testBuckets 4/3 ",
//     "--cluster true --testBuckets 4/4 "],
//    ["overal", 'config.upgrade.authentication.authentication_parameters.arangobench', ""],
//    ["dump_import", 'dump.importing', "", "--cluster true"],
//    ["shell_server", 'shell_server', "",
//     "--cluster true --testBuckets 4/1 ",
//     "--cluster true --testBuckets 4/2 ",
//     "--cluster true --testBuckets 4/3 ",
//     "--cluster true --testBuckets 4/4 "],
//    ["arangosh", 'arangosh', "",
//     "--cluster true --testBuckets 4/1 ",
//     "--cluster true --testBuckets 4/2 ",
//     "--cluster true --testBuckets 4/3 ",
//     "--cluster true --testBuckets 4/4 "],
  ]

  print("getting keyset\n")
  m = testCaseSets.size()
  int n = 0;
  for (int i = 0; i < m; i++) {
    def unitTestSet = testCaseSets.getAt(i);
    o = unitTestSet.size()
    def unitTests = unitTestSet.getAt(1);
    def shortName = unitTestSet.getAt(0);
    for (int j = 2; j < o; j ++ ) {
      def cmdLineArgs = unitTestSet.getAt(j)
      echo " ${shortName} ${cmdLineArgs} -  ${j}"
      testRunName = "${shortName}_${j}_${n}"
      paralellJobNames[n]=testRunName
      
      //      branches[testRunName] = {
        node {
          sh "pwd"
          dir("${testRunName}") {
            echo "${unitTests}"
            echo "${env}"
            docker.withRegistry(REGISTRY_URL, '') {
              def myRunImage = docker.image("${DOCKER_CONTAINER}/run")
              myRunImage.pull()
              docker.image(myRunImage.imageName()).inside('--volume /mnt/data/fileserver:/net/fileserver:rw --volume /jenkins:/mnt/:rw') {
                sh "cat /etc/issue /mnt/workspace/issue"

                echo "${env}"
                //test = new testRunner(LOCAL_TAR_DIR, MD5SUM, env.JOB_NAME, testRunName, OS, testRunName, "", unitTests, cmdLineArgs)
                //test.copyExtractTarBall()
                //test.setupTestArea()
                //test.runTests()

              }
            }
          }
          //}
        n += 1
      }
    }
  }
  echo branches.toString();
  
  // parallel branches
  branches['ssl_server']()
} catch (err) {
  stage('Send Notification unittest' )
  mail (to: ADMIN_ACCOUNT,
        subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) 'running unittest' has had a FATAL error.", 
        body: err.getMessage());
  currentBuild.result = 'FAILURE'
  throw(err)
}

stage("generating test report")
  node {
    if (failures.size() > 5) {
      def gitRange = ""
      if (lastKnownGitRev.size() > 1) {
        gitRange = "${lastKnownGitRev}.."
      }
      gitRange = "${gitRange}${currentGitRev}"
      print(gitRange)
      def gitcmd = 'git --no-pager show -s --format="%ae>" ${gitRange} |sort -u |sed -e :a -e \'$!N;s/\\n//;ta\' -e \'s;>;, ;g\' -e \'s;, $;;\''
      print(gitcmd)
      gitCommitters = sh(returnStdout: true, script: gitcmd)
      echo gitCommitters
      
      def subject = ""
      if (fatalError) {
        subject = "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed MISERABLY! "
      }
      else {
        subject = "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed"
      }
      
      mail (to: gitCommitters,
            subject: subject,
            body: "the failed testcases gave this output: ${failures}\nPlease go to ${env.BUILD_URL}.");
    }
    else {
      writeFile(lastKnownGoodGitFile, currentGitRev);
    }
  }

