class Enums

    @sortDirections: -> return [
                        {user: 'Ascending', value: 'ASC'}, 
                        {user: 'Descending', value: 'DESC'}]

    @extendedTaskState:  -> return [
                        {user: 'Failed', value: 'TASK_FAILED'},
                        {user: 'Finished', value: 'TASK_FINISHED'},
                        {user: 'Killed', value: 'TASK_KILLED'},
                        {user: 'Lost', value: 'TASK_LOST'}, 
                        {user: 'Lost While Down', value: 'TASK_LOST_WHILE_DOWN'}]

module.exports = Enums
