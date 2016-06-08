import React from 'react';
import Utils from '../../utils';
import Enums from './Enums';
import TaskSearchForm from './TaskSearchForm';
import DisplayResults from './DisplayResults';
import Header from './Header';

let TaskSearch = React.createClass({

    countChoices: [5, 10, 25],

    defaultCount: 10,

    defaultSortDirection: 'DESC',

    getInitialState() {
        return {
            form: {
                requestId: this.props.initialRequestId || '',
                deployId: this.props.initialDeployId || '',
                host: this.props.initialHost || '',
                lastTaskStatus: this.props.initialTaskStatus || '',
                startedBefore: this.props.initialStartedBefore || '',
                startedAfter: this.props.initialStartedAfter || ''
            },
            sortDirection: this.props.initialSortDirection || this.defaultSortDirection,
            queryParams: {
                requestId: this.props.initialRequestId || '',
                deployId: this.props.initialDeployId || '',
                host: this.props.initialHost || '',
                lastTaskStatus: this.props.initialTaskStatus || '',
                startedBefore: this.props.initialStartedBefore || '',
                startedAfter: this.props.initialStartedAfter || ''
            },
            pageNumber: 1,
            count: this.props.initialCount || this.defaultCount,
            hasDoneAnySearch: false
        };
    },

    handleSubmit(event) {
        event.preventDefault();
        return this.setState({
            queryParams: this.state.form,
            pageNumber: 1, // If you narrow down your search you most likely want to go back to page 1
            hasDoneAnySearch: true
        });
    },

    isAnyQueryParams() {
        return this.state.queryParams.requestId || this.state.queryParams.deployId || this.state.queryParams.host || this.state.queryParams.lastTaskStatus || this.state.queryParams.startedBefore || this.state.queryParams.startedAfter;
    },

    // Annoying that we need a new function for each property.
    // Unfortuantely using a curried function doesn't seem to work.
    updateReqeustId(event) {
        if (this.props.global) {
            let form = $.extend({}, this.state.form);
            form.requestId = event.target.value;
            return this.setState({ form });
        }
    },

    updateDeployId(event) {
        let form = $.extend({}, this.state.form);
        form.deployId = event.target.value;
        return this.setState({ form });
    },

    updateHost(event) {
        let form = $.extend({}, this.state.form);
        form.host = event.target.value;
        return this.setState({ form });
    },

    updateLastTaskStatus(event) {
        let form = $.extend({}, this.state.form);
        form.lastTaskStatus = event.target.value;
        return this.setState({ form });
    },

    updateStartedBefore(event) {
        let form = $.extend({}, this.state.form);
        form.startedBefore = event.date;
        return this.setState({ form });
    },

    updateStartedAfter(event) {
        let form = $.extend({}, this.state.form);
        form.startedAfter = event.date;
        return this.setState({ form });
    },

    resetForm() {
        return this.setState(this.getInitialState());
    },

    updateSortDirection(event) {
        if (this.state.sortDirection === Enums.sortDirections()[0].value) {
            return this.setState({ sortDirection: Enums.sortDirections()[1].value });
        } else {
            return this.setState({ sortDirection: Enums.sortDirections()[0].value });
        }
    },

    updatePageNumber(event) {
        return this.setState({ pageNumber: event.target.value });
    },

    increasePageNumber(event) {
        return this.setState({ pageNumber: this.state.pageNumber + 1 });
    },

    setPageNumber(pageNumber) {
        if (pageNumber > 0) {
            return this.setState({ pageNumber });
        }
    },

    decreasePageNumber(event) {
        if (this.state.pageNumber > 1) {
            return this.setState({ pageNumber: this.state.pageNumber - 1 });
        }
    },

    updateCount(newCount) {
        return this.setState({ count: newCount });
    },

    clearRequestId(event) {
        if (this.props.global) {
            return this.setState({ requestId: '' });
        }
    },

    clearDeployId(event) {
        return this.setState({ deployId: '' });
    },

    clearHost(event) {
        return this.setState({ host: '' });
    },

    clearSortDirection(event) {
        return this.setState({ sortDirection: '' });
    },

    clearLastTaskStatus(event) {
        return this.setState({ lastTaskStatus: '' });
    },

    clearStartedBefore(event) {
        return this.setState({ startedBefore: '' });
    },

    clearStartedAfter(event) {
        return this.setState({ startedAfter: '' });
    },

    render() {
        return <div><Header global={this.props.global} requestId={this.props.initialRequestId} /><h2> Search Parameters </h2><TaskSearchForm handleSubmit={this.handleSubmit} requestId={this.state.form.requestId} requestIdCurrentSearch={this.state.queryParams.requestId} global={this.props.global} updateReqeustId={this.updateReqeustId} deployId={this.state.form.deployId} updateDeployId={this.updateDeployId} deployIdCurrentSearch={this.state.queryParams.deployId} host={this.state.form.host} updateHost={this.updateHost} hostCurrentSearch={this.state.queryParams.host} lastTaskStatus={this.state.form.lastTaskStatus} updateLastTaskStatus={this.updateLastTaskStatus} lastTaskStatusCurrentSearch={this.state.queryParams.lastTaskStatus} startedAfter={this.state.form.startedAfter} updateStartedAfter={this.updateStartedAfter} startedAfterCurrentSearch={this.state.queryParams.startedAfter ? this.state.queryParams.startedAfter.format(window.config.timestampFormat) : undefined} startedBefore={this.state.form.startedBefore} updateStartedBefore={this.updateStartedBefore} startedBeforeCurrentSearch={this.state.queryParams.startedBefore ? this.state.queryParams.startedBefore.format(window.config.timestampFormat) : undefined} resetForm={this.resetForm} /><h2>Tasks</h2><DisplayResults requestId={this.state.queryParams.requestId} deployId={this.state.queryParams.deployId} host={this.state.queryParams.host} lastTaskStatus={this.state.queryParams.lastTaskStatus} startedAfter={this.state.queryParams.startedAfter} startedBefore={this.state.queryParams.startedBefore} sortDirection={this.state.sortDirection} increasePageNumber={this.increasePageNumber} setPageNumber={this.setPageNumber} decreasePageNumber={this.decreasePageNumber} page={this.state.pageNumber} count={this.state.count} updateCount={this.updateCount} countChoices={this.countChoices} updateSortDirection={this.updateSortDirection} clearRequestId={this.clearRequestId} clearDeployId={this.clearDeployId} clearHost={this.clearHost} clearLastTaskStatus={this.clearLastTaskStatus} clearStartedAfter={this.clearStartedAfter} clearStartedBefore={this.clearStartedBefore} clearSortDirection={this.clearSortDirection} global={this.props.global} hasDoneAnySearch={this.state.hasDoneAnySearch} holdOffOnSearching={!(this.isAnyQueryParams() || this.state.hasDoneAnySearch)} /></div>;
    }
});

export default TaskSearch;

