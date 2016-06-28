import React from 'react';
import Table from '../common/Table';
import Link from '../common/atomicDisplayItems/Link';
import PlainText from '../common/atomicDisplayItems/PlainText';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Utils from '../../utils';

let TaskFileBrowser = React.createClass({

    sortBy(field, sortDirectionAscending) {
        this.props.collection.sortBy(field, sortDirectionAscending);
        return this.forceUpdate();
    },

    columns() {
        let { sortBy } = this; // JS is annoying
        return [{
            data: 'Name',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('name', sortDirectionAscending)
        }, {
            data: 'Size',
            className: 'hidden-xs',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('size', sortDirectionAscending)
        }, {
            data: 'Last Modified',
            className: 'hidden-xs',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('mtime', sortDirectionAscending)
        }, {
            className: 'hidden-xs'
        }];
    },

    tableData() {
        let tableData = [];
        this.props.collection.map(file => {
            let FileNameComponent = file.attributes.isDirectory || file.attributes.isTailable ? Link : PlainText;
            if (file.attributes.isDirectory) {
                var onClick = event => this.navigate(file.attributes.uiPath, event);
            } else {
                var url = `${ config.appRoot }/task/${ this.props.task.taskId }/tail/${ Utils.substituteTaskId(file.attributes.uiPath, this.props.task.taskId) }`;
            }
            let size = file.attributes.isDirectory ? '' : Utils.humanizeFileSize(file.attributes.size);
            let row = {
                dataId: file.attributes.name,
                dataCollection: 'files',
                data: [{
                    component: FileNameComponent,
                    prop: {
                        text: <span><Glyphicon iconClass={file.attributes.isDirectory ? 'folder-open' : 'file'} /> {file.attributes.name}</span>,
                        url,
                        onClickFn: onClick
                    }
                }, {
                    component: PlainText,
                    className: 'hidden-xs',
                    prop: {
                        text: size
                    }
                }, {
                    component: TimeStamp,
                    className: 'hidden-xs',
                    prop: {
                        timestamp: file.attributes.mtime,
                        display: 'absoluteTimestamp'
                    }
                }]
            };
            if (file.attributes.isDirectory) {
                row.data.push({
                    component: PlainText,
                    prop: {
                        text: ''
                    }
                });
            } else {
                row.data.push({
                    component: Link,
                    className: 'hidden-xs actions-column',
                    prop: {
                        text: <Glyphicon iconClass='download-alt' />,
                        url: file.attributes.downloadLink,
                        title: 'Download',
                        altText: `Download ${ file.attributes.name }`,
                        overlayTrigger: true,
                        overlayTriggerPlacement: 'top',
                        overlayToolTipContent: `Download ${ file.attributes.name }`,
                        overlayId: `downloadFile${ file.attributes.name }`
                    }
                });
            }
            return tableData.push(row);
        });
        return tableData;
    },

    emptyTableMessage() {
        let { task, slaveOffline } = this.props;
        let emptyTableMessage = 'No files exist in task directory.';

        if (task.get('taskUpdates') && task.get('taskUpdates').length > 0) {
            switch (_.last(task.get('taskUpdates')).taskState) {
                case 'TASK_LAUNCHED':case 'TASK_STAGING':case 'TASK_STARTING':
                    emptyTableMessage = 'Could not browse files. The task is still starting up.';break;
                case 'TASK_KILLED':case 'TASK_FAILED':case 'TASK_LOST':case 'TASK_FINISHED':
                    emptyTableMessage = 'No files exist in task directory. It may have been cleaned up.';break;
            }
        }

        if (slaveOffline) {
            emptyTableMessage = `Task files are not availible because ${ this.props.task.attributes.task.taskId.sanitizedHost } is offline.`;
        }
        return emptyTableMessage;
    },

    navigate(path, event) {
        event.preventDefault();

        this.props.collection.path = `${ path }`;

        app.router.navigate(`#task/${ this.props.collection.taskId }/files/${ this.props.collection.path }`);

        this.props.collection.fetch({
            reset: true,
            done: this.forceUpdate
        });

        return this.clearTableSort();
    },

    renderBreadcrumbs() {
        let breadcrumbs = [];
        this.props.breadcrumbs.map((breadcrumb, key) => {
            return breadcrumbs.push(React.createElement("li", { "key": key }, React.createElement("a", { ["onClick"]: event => this.navigate(breadcrumb.path, event) }, breadcrumb.name)));
        });
        return breadcrumbs;
    },

    renderTable() {
        return React.createElement(Table, {
            "noPages": true,
            "tableClassOpts": "table-striped files-table sortable-theme-bootstrap",
            "columnHeads": this.columns(),
            "tableRows": this.tableData(),
            "emptyTableMessage": this.emptyTableMessage(),
            "dataCollection": 'taskFiles',
            ["ref"]: table => {
                if (table) {
                    return this.clearTableSort = table.clearSort;
                }
            }
        });
    },

    renderBreadcrumbsAndTable() {
        if (this.props.task.slaveMissing) {
            return <div className="empty-table-message">Files can not be fetched because the slave is no longer available</div>;
        } else {
            return <div><ul className="breadcrumb">{this.renderBreadcrumbs()}</ul>{this.renderTable()}</div>;
        }
    },

    render() {
        return <div><div className="page-header file-browser-header"><h2>Files</h2></div>{this.renderBreadcrumbsAndTable()}</div>;
    }
});

export default TaskFileBrowser;

