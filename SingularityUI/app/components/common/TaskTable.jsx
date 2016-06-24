import React from 'react';
import OldTable from './OldTable';
import TimeStamp from './atomicDisplayItems/TimeStamp';
import TaskStateLabel from './atomicDisplayItems/TaskStateLabel';
import Link from './atomicDisplayItems/Link';
import IconButton from './atomicDisplayItems/IconButton';
import Glyphicon from './atomicDisplayItems/Glyphicon';
import PlainText from './atomicDisplayItems/PlainText';
import Utils from '../../utils';

let TaskTable = React.createClass({

    /* 
    NOTE: @props.sortByX, if provided, should do at least three things:
        - explicitly set @props.sortDirection
        - explicitly set @props.sortBy
        - sort @props.models
    */
    render() {
        let taskTableColumns = [{}, {
            data: 'Request ID',
            className: 'hidden-sm hidden-xs',
            sortable: this.props.sortableByRequestId,
            doSort: this.props.sortByRequestId,
            sortAttr: 'requestId'
        }, {
            data: 'Deploy ID',
            className: 'hidden-sm hidden-xs',
            sortable: this.props.sortableByDeployId,
            doSort: this.props.sortByDeployId,
            sortAttr: 'deployId'
        }, {
            data: 'Host',
            className: 'hidden-sm hidden-xs',
            sortable: this.props.sortableByHost,
            doSort: this.props.sortByHost,
            sortAttr: 'host'
        }, {
            data: 'Last Status',
            className: 'hidden-sm hidden-xs',
            sortable: this.props.sortableByLastStatus,
            doSort: this.props.sortByLastStatus,
            sortAttr: 'lastTaskState'
        }, {
            data: 'Started',
            className: 'hidden-sm hidden-xs',
            sortable: this.props.sortableByStarted,
            doSort: this.props.sortByStarted,
            sortAttr: 'startedAt'
        }, {
            data: 'Updated',
            className: 'hidden-xs',
            sortable: this.props.sortableByUpdated,
            doSort: this.props.sortByUpdated,
            sortAttr: 'updatedAt'
        }, {
            className: 'hidden-xs'
        }, {
            className: 'hidden-xs'
        }];
        let taskTableData = [];
        this.props.models.map(function (task) {
            let viewJsonFn = event => Utils.viewJSON(task);
            return taskTableData.push({
                dataId: task.taskId.id,
                dataCollection: 'taskHistory',
                data: [{
                    component: Link,
                    className: 'actions-column',
                    id: `linkForTask${ task.taskId.id }`,
                    prop: {
                        text: <Glyphicon iconClass='link' />,
                        url: `${ window.config.appRoot }/task/${ task.taskId.id }`,
                        altText: `Task ${ task.taskId.id }`,
                        title: "Go To Task",
                        overlayTrigger: true,
                        overlayTriggerPlacement: 'top',
                        overlayToolTipContent: 'Go To Task',
                        overlayId: `overlayForLinkToTask${ task.taskId.id }`
                    }
                }, {
                    component: Link,
                    className: 'hidden-sm hidden-xs long-link',
                    prop: {
                        text: task.taskId.requestId,
                        url: `${ window.config.appRoot }/request/${ task.taskId.requestId }/`,
                        altText: `Request ${ task.taskId.requestId }`
                    }
                }, {
                    component: Link,
                    className: 'hidden-sm hidden-xs',
                    prop: {
                        text: task.taskId.deployId,
                        url: `${ window.config.appRoot }/request/${ task.taskId.requestId }/deploy/${ task.taskId.deployId }`,
                        altText: `Deploy ${ task.taskId.deployId }`
                    }
                }, {
                    component: PlainText,
                    className: 'hidden-sm hidden-xs',
                    prop: {
                        text: task.taskId.host
                    }
                }, {
                    component: TaskStateLabel,
                    className: 'hidden-sm hidden-xs',
                    prop: {
                        taskState: task.lastTaskState
                    }
                }, {
                    component: TimeStamp,
                    className: 'hidden-sm hidden-xs',
                    prop: {
                        timestamp: task.taskId.startedAt,
                        display: 'timeStampFromNow'
                    }
                }, {
                    component: TimeStamp,
                    className: 'hidden-xs',
                    prop: {
                        timestamp: task.updatedAt,
                        display: 'timeStampFromNow'
                    }
                }, {
                    component: Link,
                    className: 'hidden-xs actions-column',
                    prop: {
                        text: <Glyphicon iconClass='option-horizontal' />,
                        url: `${ window.config.appRoot }/request/${ task.taskId.requestId }/tail/stdout/?taskIds=${ task.taskId.id }`,
                        title: 'Log',
                        altText: `Logs for task ${ task.taskId.id }`,
                        overlayTrigger: true,
                        overlayTriggerPlacement: 'top',
                        overlayToolTipContent: 'Logs',
                        overlayId: `overlayForLogsOfTask${ task.taskId.id }`
                    }
                }, {
                    component: Link,
                    className: 'hidden-xs actions-column',
                    prop: {
                        text: '{ }',
                        url: '#',
                        title: 'JSON',
                        onClickFn: viewJsonFn,
                        altText: `View JSON for task ${ task.taskId.id }`,
                        id: task.taskId.id,
                        overlayTrigger: true,
                        overlayTriggerPlacement: 'top',
                        overlayToolTipContent: 'JSON',
                        overlayId: `overlayForJSONOfTask${ task.taskId.id }`
                    }
                }] });
        });
        return <OldTable tableClassOpts='table-striped' columnHeads={taskTableColumns} tableRows={taskTableData} sortDirection={this.props.sortDirection} sortDirectionAscending={this.props.sortDirectionAscending} sortBy={this.props.sortBy} emptyTableMessage={this.props.emptyTableMessage || 'No Tasks'} customSorting={true} customPaging={true} rowsPerPageChoices={this.props.rowsPerPageChoices} setRowsPerPage={this.props.setRowsPerPage} pageNumber={this.props.pageNumber} pageDown={this.props.pageDown} pageUp={this.props.pageUp} dataCollection='taskHistory' />;
    }
});

export default TaskTable;

