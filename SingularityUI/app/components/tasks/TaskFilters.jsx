import React from 'react';
import Utils from '../../utils';

import { Nav, NavItem, OverlayTrigger, Tooltip, Glyphicon } from 'react-bootstrap';

export default class TaskFilters extends React.Component {

  static get TASK_STATES() {
    return [
      {
        filterVal: 'active',
        displayVal: 'Active',
        tip: 'Currently running'
      },
      {
        filterVal: 'scheduled',
        displayVal: 'Scheduled',
        tip: 'Will run at next scheduled interval'
      },
      {
        filterVal: 'cleaning',
        displayVal: 'Cleaning',
        tip: 'Have been asked to shut down'
      },
      {
        filterVal: 'lbcleanup',
        displayVal: 'LB Cleaning',
        tip: 'Being removed from the load balancer'
      },
      {
        filterVal: 'decommissioning',
        displayVal: 'Decommissioning',
        tip: 'Being killed due to a decommissioning slave'
      }
    ];
  }

  static get REQUEST_TYPES() {
    return ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];
  }

  handleStatusSelect(selectedKey) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {taskStatus: TaskFilters.TASK_STATES[selectedKey].filterVal}));
  }

  handleSearchChange(event) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {filterText: event.target.value}));
  }

  clearSearch() {
    this.props.onFilterChange(_.extend({}, this.props.filter, {filterText: ''}));
  }

  toggleRequestType(requestType) {
    let selected = this.props.filter.requestTypes;
    if (selected.length === TaskFilters.REQUEST_TYPES.length) {
      selected = [requestType];
    } else if (_.isEmpty(_.without(selected, requestType))) {
      selected = TaskFilters.REQUEST_TYPES;
    } else if (_.contains(selected, requestType)) {
      selected = _.without(selected, requestType);
    } else {
      selected.push(requestType);
    }
    this.props.onFilterChange(_.extend({}, this.props.filter, {requestTypes: selected}));
  }

  renderStatusFilter() {
    const selectedIndex = _.findIndex(TaskFilters.TASK_STATES, (taskState) => taskState.filterVal === this.props.filter.taskStatus);
    const navItems = TaskFilters.TASK_STATES.map((taskState, index) => {
      return (
        <OverlayTrigger key={index} placement="top" overlay={<Tooltip id={index}>{taskState.tip}</Tooltip>} delay={500}>
          <NavItem
            eventKey={index}
            title={taskState.tip}
            active={index === selectedIndex}
            onClick={() => this.handleStatusSelect(index)}>
              {taskState.displayVal}
          </NavItem>
        </OverlayTrigger>
      );
    });

    return (
      <Nav bsStyle="pills" activeKey={selectedIndex}>
        {navItems}
      </Nav>
    );
  }

  renderSearchInput() {
    return (
      <div>
        <input
          type="search"
          ref="search"
          className="big-search-box"
          placeholder="Filter tasks"
          value={this.props.filter.filterText}
          onChange={(...args) => this.handleSearchChange(...args)}
          maxLength="128"
        />
        <div className="remove-button" onClick={() => this.clearSearch()}></div>
      </div>
    );
  }

  renderRequestTypeFilter() {
    const filterItems = this.props.displayRequestTypeFilters && TaskFilters.REQUEST_TYPES.map((requestType, index) => {
      const isActive = _.contains(this.props.filter.requestTypes, requestType);
      return (
        <li key={index} className={isActive ? 'active' : ''}>
          <a onClick={() => this.toggleRequestType(requestType)}>
            {isActive ? <Glyphicon glyph="ok" /> : <span className="icon-placeholder" />} {Utils.humanizeText(requestType)}
          </a>
        </li>
      );
    });

    return (
      <div className="requests-filter-container">
        <ul className="nav nav-pills nav-pills-multi-select">
          {filterItems}
        </ul>
      </div>
    );
  }

  render() {
    return (
      <div>
        <div className="row">
          <div className="col-md-12">
            {this.renderStatusFilter()}
          </div>
        </div>
        <div className="row">
          <div className="col-md-12">
            {this.renderSearchInput()}
          </div>
          <div className="col-md-12">
            {this.renderRequestTypeFilter()}
          </div>
        </div>
      </div>
    );
  }
}

TaskFilters.propTypes = {
  onFilterChange: React.PropTypes.func.isRequired,
  filter: React.PropTypes.shape({
    taskStatus: React.PropTypes.string.isRequired,
    requestTypes: React.PropTypes.array.isRequired,
    filterText: React.PropTypes.string.isRequired
  }).isRequired,
  displayRequestTypeFilters: React.PropTypes.bool
};
