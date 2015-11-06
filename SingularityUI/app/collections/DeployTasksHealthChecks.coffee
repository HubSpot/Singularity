Collection = require './collection'

HealthCheckResult = require '../models/HealthCheckResult'

class DeployTasksHealthChecks extends Collection

    model: HealthCheckResult

    initialize: (models = [], {}) ->


module.exports = DeployTasksHealthChecks
