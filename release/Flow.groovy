def LinuxTargets
def DOCKER_HOST_2
def DOCKER_HOST="docker"
enableDocker2=(params['HAVE_2_BUILDERS'] == 'true')
echo " ${params['HAVE_2_BUILDERS']} - ${enableDocker2}"
if (enableDocker2) {
  echo "Have 2 docker hosts!"
  DOCKER_HOST_2='docker2'
  LinuxTargets="LinuxEnterprise"
}
else {
  echo "building with one docker host"
  DOCKER_HOST_2='docker'
  LinuxTargets="linuxPackages"
}
def parts=params['GITTAG'].tokenize(".")
VERSION_MAJOR=parts[0]
VERSION_MINOR=parts[1]
VERSION_MAJOR_MINOR="${VERSION_MAJOR}.${VERSION_MINOR}"
REPO_TL_DIR="arangodb${VERSION_MAJOR}${VERSION_MINOR}"
//================================================================================
stage("building packages") {
  echo "skip build is: ${SKIP_BUILD}"
  if (SKIP_BUILD == "false") {
    echo "Now starting to build:"
    parallel(
      [
        ////////////////////////////////////////////////////////////////////////////////
        "sourceTarballs": {
          node("master") {
            sh "export GITTAG=${params['GITTAG']}; ${ARANGO_SCRIPT_DIR}/source/build.sh"

          }
        },
        ////////////////////////////////////////////////////////////////////////////////
       LinuxTargets: {
          ///----------------------------------------------------------------------
          echo "building Linux Enterprise Release"
          build( job: 'RELEASE__BuildPackages',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: params['ENTERPRISE_URL']),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: params['preferBuilder']),
		   string(name: 'DOCKER_HOST', value: DOCKER_HOST),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )

          ///----------------------------------------------------------------------
	  if (! enableDocker2) {
            echo "building Linux Community Release"
            build( job: 'RELEASE__BuildPackages',
                   parameters: [
                     string(name: 'ENTERPRISE_URL', value: ''),
                     string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                     string(name: 'preferBuilder', value: params['preferBuilder']),
		     string(name: 'DOCKER_HOST', value: DOCKER_HOST),
                     booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                   ]
                 )
	  }

        },
        ////////////////////////////////////////////////////////////////////////////////
        "linuxCommunityPackages": {
          ///----------------------------------------------------------------------
	  if (enableDocker2) {
            echo "building Linux Community Release"
            build( job: 'RELEASE__BuildPackages',
                   parameters: [
                     string(name: 'ENTERPRISE_URL', value: ''),
                     string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                     string(name: 'preferBuilder', value: params['preferBuilder']),
		     string(name: 'DOCKER_HOST', value: DOCKER_HOST_2),
                     booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                   ]
                 )
	  }

        },
        ////////////////////////////////////////////////////////////////////////////////
        "macintosh": {
          echo "building MacOS X Enterprise Release"
          ///----------------------------------------------------------------------
          node("macos") {
            sh "mkdir -p /Users/jenkins/net/fileserver/; rm -rf /Users/jenkins/net/fileserver/*"
          }
          ///----------------------------------------------------------------------
          build( job: 'RELEASE__BuildPackages',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: params['ENTERPRISE_URL']),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'macos'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )

          ///----------------------------------------------------------------------
          echo "building MacOS X Community Release"
          build( job: 'RELEASE__BuildPackages',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: ''),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'macos'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )

          ///----------------------------------------------------------------------
          echo "uploading dmg's"
          node("macos") {
            sh "scp -r /Users/jenkins/net/fileserver/* ${env.JENKINSMASTER}:${env.INTERMEDIATE_DIR}"
          }
          ///----------------------------------------------------------------------
          echo "running MacOS X Release unittests"
          build( job: 'RELEASE__BuildTest',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: ''),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'macos'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )
          
          build( job: 'RELEASE__BuildTest',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: params['ENTERPRISE_URL']),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'macos'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )
          
        },
        ////////////////////////////////////////////////////////////////////////////////
        "Windows": {
          node('windows') {
            sh "rm -rf /var/tmp/r/; mkdir -p /var/tmp/r/"
          }
          // Windows doesn't like if we compile multiple times at once...
          ///----------------------------------------------------------------------
          echo "building Windows Enterprise Release"
          build( job: 'RELEASE__BuildPackages',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: params['ENTERPRISE_URL']),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'windows'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )

          ///----------------------------------------------------------------------
          echo "building Windows Community Release"
          build( job: 'RELEASE__BuildPackages',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: ''),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'windows'),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )
          node('windows') {
            sh "scp -r /var/tmp/r/*  ${JENKINSMASTER}:/mnt/data/fileserver/"
            sh "/usr/bin/rsync -ua /cygdrive/e/symsrv ${JENKINSMASTER}:${PUBLIC_CO_DIR}"
          }
        },
        ////////////////////////////////////////////////////////////////////////////////
        "documentation": {
          build( job: 'RELEASE__BuildDocumentation',
                 parameters: [
                   string(name: 'ENTERPRISE_URL', value: params['ENTERPRISE_URL']),
                   string(name: 'GITTAG', value: "v${params['GITTAG']}"),
                   string(name: 'preferBuilder', value: 'debianjessieDocu'),
                   string(name: 'FORCE_GITBRANCH', value:''),
                   booleanParam(name: 'CLEAN_BUILDENV', value: params['CLEAN_BUILDENV'])
                 ]
               )
        }
      ]
    )
  }
}
//================================================================================

stage("create repositories") {
  if (SKIP_REPOBUILD == 'false') {
    build(
      job: 'RELEASE__BuildRepositories',
          parameters: [
            string(name: 'GITTAG', value: params['GITTAG']),
            string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}"),
            booleanParam(name: 'DEBUG', value: false)
            
          ]
    )
  }
}    

stage("Build Travis CI") {
  node('master') {
    build(
      job: 'RELEASE__BuildTravisCI',
          parameters: [
            string(name: 'GITTAG', value: params['GITTAG']),
            string(name: 'DEBFILE', value: "${env.INTERMEDIATE_CO_DIR}/${REPO_TL_DIR}/xUbuntu_12.04/amd64/arangodb3-${GITTAG}-*_amd64.deb"),
            booleanParam(name: 'UPDATE_LINK', value: true),
            booleanParam(name: 'DEBUG', value: false)
          ]
    )
  }
}

stage("Generating HTML snippets & test it with the packages") {
  build(
    job: 'RELEASE__CreateDownloadSnippets',
        parameters: [
          string(name: 'GITTAG', value: params['GITTAG']),
          string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}")
        ]
  )

  if (SKIP_INSTALL_TEST == "false") {
    build(
      job: 'RELEASE__TestPackages',
          parameters: [
            string(name: 'preferBuilder', value: ''),
            string(name: 'GITTAG', value: params['GITTAG']),
            string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}"),
            booleanParam(name: 'DEBUG', value: false),
            booleanParam(name: 'testLiveDownloads', value: false)
          ]
    )
  }
}
////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
input("message": "Everything we did so far was private. Proceed to the publish step now?")
////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////

stage("publish packages") {
  node('master') {
    sh "export REPO_TL_DIR=${REPO_TL_DIR}; ${ARANGO_SCRIPT_DIR}/publish/stage2public.sh"
    sh "export REPO_TL_DIR=${REPO_TL_DIR}; ${ARANGO_SCRIPT_DIR}/publish/publish_documentation.sh"
    sh "echo '${params['GITTAG']}' > ${env.PUBLIC_CO_DIR}VERSION"
  }
}

stage("updating other repos") {
  parallel(
    [
      ////////////////////////////////////////////////////////////////////////////////
      "homeBrew": {
        node('macos') {
          if (IS_RELEASE == 'true') {
            build( job: 'RELEASE__BuildHomebrew',
                   parameters: [
                     string(name: 'GITTAG', value: params['GITTAG']),
                     booleanParam(name: 'DEBUG', value: false),
                   ]
                 )
          }
        }
      },
      "AMI": {
        node('master') {
          if (IS_RELEASE == 'true') {
            build( job: 'RELEASE__BuildAMI',
                   parameters: [
                     string(name: 'GITTAG', value: params['GITTAG']),
                     string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}"),
                     booleanParam(name: 'NEW_MAJOR_RELEASE', value: false),
                     booleanParam(name: 'DEBUG', value: false)
                   ]
                 )
          }
        }
      },
      "SNAPPY": {
        node('master') {
          build( job: 'RELEASE__PublishSnap',
                 parameters: [
                   string(name: 'GITTAG', value: params['GITTAG']),
                   string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}"),
                 ]
               )
        }
      },
      "Docker": {
        if (SKIP_DOCKER_PUBLISH == 'false' && IS_RELEASE == 'true') {
          node('master') {
            build( job: 'RELEASE__UpdateDockerResources',
                   parameters: [
                     string(name: 'GITTAG', value: params['GITTAG']),
                     string(name: 'REPO_TL_DIR', value: "${REPO_TL_DIR}"),
                     booleanParam(name: 'NEW_MAJOR_RELEASE', value: params['NEW_MAJOR_RELEASE']),
                     booleanParam(name: 'UPDATE_MESOS_IMAGE', value: true),
                     booleanParam(name: 'UPDATE_UNOFFICIAL_IMAGE', value: true),
                     booleanParam(name: 'CREATE_DOCKER_LIBRARY_PULLREQ', value: true),
                     booleanParam(name: 'CREATE_NEW_VERSION', value: true),
                     booleanParam(name: 'DEBUG', value: false)
                   ]
                 )
          }
        }
      },
      "Update Github Master": {
        if (IS_RELEASE == 'true') {
          node("master") {
            build( job: 'RELEASE__UpdateGithubMaster',
                   parameters: [
                     string(name: 'GITTAG', value: params['GITTAG'])
                   ]
                 )
          }
        }
      },
      "Update Github Unstable": {
        node("master") {
          build( job: 'RELEASE__UpdateGithubUnstable',
                 parameters: [
                   string(name: 'GITTAG', value: params['GITTAG'])
                 ]
               )
        }
      }
    ])
}
