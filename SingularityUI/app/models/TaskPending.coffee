Model = require './model'

Request = require './Request'

class TaskPending extends Model
    # Won't be displayed in JSON dialog
    ignoreAttributes: ['id', 'host', 'cpus', 'memoryMb']   	

module.exports = TaskPending