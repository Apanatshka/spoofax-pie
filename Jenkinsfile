#!groovy
@Library('metaborg.jenkins.pipeline@develop') _

gradlePipeline(
  buildDevelopBranch: false,
  buildOtherBranch: false,
  buildTag: false,
  gradleParallel: false,
  gradleMaxWorkers: '1',
  slack: true,
  slackChannel: '#spoofax3-dev'
)
