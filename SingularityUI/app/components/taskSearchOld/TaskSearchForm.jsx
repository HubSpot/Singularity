import React from 'react';
import Utils from '../../utils';

import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import DateEntry from '../common/formItems/DateEntry';
import LinkedFormItem from '../common/formItems/LinkedFormItem';
import Enums from './Enums';

let TaskSearchForm = React.createClass({

    getRequestIdTitle() {
        return <div>Request ID{this.props.requestIdCurrentSearch ? <span className='badge current-query-param'>{this.props.requestIdCurrentSearch}</span> : undefined}</div>;
    },

    getDeployIdTitle() {
        return <div>Deploy ID{this.props.deployIdCurrentSearch ? <span className='badge current-query-param'>{this.props.deployIdCurrentSearch}</span> : undefined}</div>;
    },

    getHostTitle() {
        return <div>Host{this.props.hostCurrentSearch ? <span className='badge current-query-param'>{this.props.hostCurrentSearch}</span> : undefined}</div>;
    },

    getStartedBetweenTitle() {
    	return <div>TODO: fix</div>
    },
    /*    return <div>
            Started Between
            {if (this.props.startedAfterCurrentSearch && this.props.startedBeforeCurrentSearch) {
                return <span className='badge current-query-param'>{@props.startedAfterCurrentSearch} - {@props.startedBeforeCurrentSearch}</span>
            } else if (this.props.startedAfterCurrentSearch) {
                return <span className='badge current-query-param'>After {@props.startedAfterCurrentSearch}</span>
            } else if (this.props.startedBeforeCurrentSearch) {
                <span className='badge current-query-param'>Before {@props.startedBeforeCurrentSearch}</span>
        	}}
        </div>
    },*/

    getLastTaskStatusTitle() {
        return <div>Last Task Status{this.props.lastTaskStatusCurrentSearch ? <span className='badge current-query-param'>{this.props.lastTaskStatusCurrentSearch}</span> : undefined}</div>;
    },

    render() {
        ({ render() {} });
        return <div className='jumbotron col-md-12'><form role='form' onSubmit={this.props.handleSubmit} className='form-vertical'><div className='row'><div className='col-md-4'><label htmlFor='requestId'>{this.getRequestIdTitle()}</label><FormField title={this.getRequestIdTitle()} id='requestId' prop={{
                            value: this.props.requestId,
                            inputType: 'text',
                            disabled: !this.props.global,
                            updateFn: this.props.updateReqeustId
                        }} /></div><div className='col-md-4'><label htmlFor='deployId'>{this.getDeployIdTitle()}</label><FormField title={this.getDeployIdTitle()} id='deployId' prop={{
                            value: this.props.deployId,
                            inputType: 'text',
                            updateFn: this.props.updateDeployId
                        }} /></div><div className='col-md-4'><label htmlFor='host'>{this.getHostTitle()}</label><FormField title={this.getHostTitle()} id='host' prop={{
                            value: this.props.host,
                            inputType: 'text',
                            updateFn: this.props.updateHost
                        }} /></div></div><div className='row'><div className='col-md-4'><label htmlFor='executionStartedBetween'>{this.getStartedBetweenTitle()}</label><LinkedFormItem id='executionStartedBetween' title={this.getStartedBetweenTitle()} prop={{
                            customClass: 'form-inline',
                            formItem1: {
                                component: DateEntry,
                                title: 'Started After',
                                id: 'startedAfter',
                                prop: {
                                    customClass: 'col-xs-5 pull-left',
                                    value: this.props.startedAfter,
                                    inputType: 'datetime',
                                    updateFn: this.props.updateStartedAfter
                                }
                            },
                            separator: '-',
                            formItem2: {
                                component: DateEntry,
                                title: 'Started Before',
                                id: 'startedBefore',
                                prop: {
                                    customClass: 'col-xs-5 pull-right',
                                    value: this.props.startedBefore,
                                    inputType: 'datetime',
                                    updateFn: this.props.updateStartedBefore
                                }
                            }
                        }} /></div><div className='col-md-4'><label htmlFor='lastTaskStatus'>{this.getLastTaskStatusTitle()}</label><DropDown id='lastTaskStatus' prop={{
                            value: this.props.lastTaskStatus,
                            choices: Enums.extendedTaskState(),
                            inputType: 'text',
                            updateFn: this.props.updateLastTaskStatus
                        }} /></div><label htmlFor='buttons'> </label><div className='col-md-4' id='buttons'><div className='pull-right'><div className='col-md-3'><button type='submit' className='btn btn-primary'>Search</button></div></div><div className='pull-right'><div className='col-md-3'><button type='button' className='btn btn-danger' onClick={this.props.resetForm}>Clear</button></div></div></div></div></form></div>;
    }
});

export default TaskSearchForm;

