import React from 'react';
import HistoricalTasks from '../../collections/HistoricalTasks';

import QueryParameters from '../common/QueryParameters';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import TaskTable from '../common/TaskTable';

import Enums from './Enums';

let DisplayResults = React.createClass({

    // Used to detect if any query params have changed
    didQueryParamsChange(nextProps) {
        if (nextProps.requestId !== this.props.requestId) {
            return true;
        }
        if (nextProps.global !== this.props.global) {
            return true;
        }
        if (nextProps.deployId !== this.props.deployId) {
            return true;
        }
        if (nextProps.host !== this.props.host) {
            return true;
        }
        if (nextProps.lastTaskStatus !== this.props.lastTaskStatus) {
            return true;
        }
        if (nextProps.startedBefore !== this.props.startedBefore) {
            return true;
        }
        if (nextProps.startedAfter !== this.props.startedAfter) {
            return true;
        }
        if (nextProps.sortDirection !== this.props.sortDirection) {
            return true;
        }
        if (nextProps.page !== this.props.page) {
            return true;
        }
        if (nextProps.count !== this.props.count) {
            return true;
        }
        return false;
    },

    getInitialState() {
        this.willFetch = false;
        return {
            loading: true
        };
    },

    getEmptyTableMessage() {
        if (this.props.holdOffOnSearching) {
            return 'Enter parameters above to view tasks.';
        } else if (this.state.loading) {
            return 'Loading Tasks...';
        } else {
            return 'No Tasks Found';
        }
    },

    fetchCollection() {
        if (!this.props.holdOffOnSearching) {
            this.willFetch = false;
        }
        this.collection = new HistoricalTasks([], {
            params: {
                requestId: this.props.requestId,
                deployId: this.props.deployId,
                host: this.props.host,
                lastTaskStatus: this.props.lastTaskStatus,
                startedBefore: this.props.startedBefore ? this.props.startedBefore.valueOf() : undefined,
                startedAfter: this.props.startedAfter ? this.props.startedAfter.valueOf() : undefined,
                orderDirection: this.props.sortDirection,
                count: this.props.count,
                page: this.props.page
            }
        });
        if (!this.props.holdOffOnSearching) {
            return this.collection.fetch({ success: () => this.setState({ loading: false }) });
        }
    },

    componentWillMount() {
        return this.fetchCollection();
    },

    componentWillReceiveProps(nextProps) {
        // Note that if adding another query param you MUST update @didQueryParamsChange
        if (this.didQueryParamsChange(nextProps) || this.props.holdOffOnSearching && !nextProps.holdOffOnSearching) {
            this.willFetch = true;
            return this.setState({ loading: true });
        }
    },

    renderPageNavBar() {
        return <TableNavigationBar currentPage={this.collection.params.page} decreasePageNumber={this.props.decreasePageNumber} increasePageNumber={this.props.increasePageNumber} setPageNumber={this.props.setPageNumber} numberPerPage={this.props.count} objectsBeingDisplayed="Tasks" numberPerPageChoices={this.props.countChoices} setNumberPerPage={this.props.updateCount} sortDirection={this.props.sortDirection} sortDirectionChoices={Enums.sortDirections()} setSortDirection={this.props.updateSortDirection} />;
    },

    render() {
        if (this.willFetch) {
            this.fetchCollection();
        }
        return <div className='col-xl-12'><TaskTable models={this.collection.models} sortDirection={this.props.sortDirection} sortDirectionAscending={Enums.sortDirections()[0].value} sortBy='Started' sortableByStarted={true} sortByStarted={this.props.updateSortDirection} rowsPerPageChoices={this.props.countChoices} setRowsPerPage={this.props.updateCount} pageNumber={this.collection.params.page} pageDown={this.props.decreasePageNumber} pageUp={this.props.increasePageNumber} emptyTableMessage={this.getEmptyTableMessage()} /></div>;
    }
});

export default DisplayResults;

