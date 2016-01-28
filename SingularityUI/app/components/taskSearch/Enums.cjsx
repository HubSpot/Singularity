class Enums

	@sortDirections: -> return [{user: 'Ascending', value: 'ASC'}, {user: 'Descending', value: 'DESC'}]

	@extendedTaskState:  -> return [
						{user: 'Cleaning', value: 'TASK_CLEANING'},
						{user: 'Failed', value: 'TASK_FAILED'},
						{user: 'Finished', value: 'TASK_FINISHED'},
						{user: 'Killed', value: 'TASK_KILLED'},
						{user: 'Launched', value: 'TASK_LAUNCHED'}, 
						{user: 'Lost', value: 'TASK_LOST'}, 
						{user: 'Lost While Down', value: 'TASK_LOST_WHILE_DOWN'},
						{user: 'Running', value: 'TASK_RUNNING'}, 
						{user: 'Staging', value: 'TASK_STAGING'}, 
						{user: 'Starting', value: 'TASK_STARTING'}]

module.exports = Enums