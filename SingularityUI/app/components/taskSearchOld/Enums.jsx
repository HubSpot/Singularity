class Enums {

                    static sortDirections() {
                                        return [{ user: 'Ascending', value: 'ASC' }, { user: 'Descending', value: 'DESC' }];
                    }

                    static extendedTaskState() {
                                        return [{ user: 'Error', value: 'TASK_ERROR' }, { user: 'Failed', value: 'TASK_FAILED' }, { user: 'Finished', value: 'TASK_FINISHED' }, { user: 'Killed', value: 'TASK_KILLED' }, { user: 'Lost', value: 'TASK_LOST' }, { user: 'Lost While Down', value: 'TASK_LOST_WHILE_DOWN' }];
                    }
}

export default Enums;

