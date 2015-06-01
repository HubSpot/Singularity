path = require 'path'
fs =   require 'fs'

handlebars = require 'handlebars-brunch/node_modules/handlebars'

# Brunch settings
exports.config =
    paths:
        public: path.resolve(__dirname, '../SingularityService/target/generated-resources/assets')

    files:
        javascripts:
            joinTo: 'static/js/app.js'
            order: before: [
                /^(bower_components|vendor)/
            ]

        stylesheets:
            joinTo: 'static/css/app.css'

        templates:
            defaultExtension: 'hbs'
            joinTo: 'static/js/app.js'

    server:
        base: process.env.SINGULARITY_BASE_URI ? '/singularity'


    # When running SingularityUI via brunch server we need to make an index.html for it
    # based on the template that's shared with SingularityService
    # 
    # After we compile the static files, compile index.html using some required configs
    onCompile: =>
        destination = path.resolve @config.paths.public, 'index.html'

        templatePath = path.resolve 'app/assets/_index.mustache'
        indexTemplate = fs.readFileSync templatePath, 'utf-8'

        templateData =
            staticRoot: process.env.SINGULARITY_STATIC_URI ? "#{ @config.server.base }/static"
            appRoot: "#{ @config.server.base }/ui"
            apiRoot: process.env.SINGULARITY_API_URI ? ''
            slaveHttpPort: 5051
            title: process.env.SINGULARITY_TITLE ? 'Singularity (local dev)'
            navColor: process.env.SINGULARITY_NAV_COLOR ? ''
            defaultCpus: 1
            defaultMemory: 128
            defaultHealthcheckIntervalSeconds: process.env.SINGULARITY_DEFAULT_HEALTHCHECK_INTERVAL_SECONDS ? 5
            defaultHealthcheckTimeoutSeconds: process.env.SINGULARITY_HEALTHCHECK_TIMEOUIT_SECONDS ? 5
            defaultDeployHealthTimeoutSeconds: process.env.SINGULARITY_DEPLOY_HEALTH_TIMEOUT_SECONDS ? 120
            hideNewDeployButton: process.env.SINGULARITY_HIDE_NEW_DEPLOY_BUTTON ? "false"
            hideNewRequestButton: process.env.SINGULARITY_HIDE_NEW_REQUEST_BUTTON ? "false"
            runningTaskLogPath:  process.env.SINGULARITY_RUNNING_TASK_LOG_PATH ? "stdout"
            finishedTaskLogPath: process.env.SINGULARITY_FINISHED_TASK_LOG_PATH ? "stdout"

        compiledTemplate = handlebars.compile(indexTemplate)(templateData)
        fs.writeFileSync destination, compiledTemplate
