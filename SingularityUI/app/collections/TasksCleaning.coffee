TasksActive = require './TasksActive'

class TasksCleaning extends TasksActive

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/tasks/cleaning"

module.exports = TasksCleaning