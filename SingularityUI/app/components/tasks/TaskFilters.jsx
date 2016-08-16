import React from 'react';
import Utils from '../../utils';
import InfoModalButton from '../common/modal/InfoModalButton';
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

  handleSearchChange(e) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {filterText: e.target.value}));
  }

  clearSearch() {
    this.props.onFilterChange(_.extend({}, this.props.filter, {filterText: ''}));
  }

  toggleRequestType(t) {
    let selected = this.props.filter.requestTypes;
    if (selected.length === TaskFilters.REQUEST_TYPES.length) {
      selected = [t];
    } else if (_.isEmpty(_.without(selected, t))) {
      selected = TaskFilters.REQUEST_TYPES;
    } else if (_.contains(selected, t)) {
      selected = _.without(selected, t);
    } else {
      selected.push(t);
    }
    this.props.onFilterChange(_.extend({}, this.props.filter, {requestTypes: selected}));
  }

  renderStatusFilter() {
    const selectedIndex = _.findIndex(TaskFilters.TASK_STATES, (s) => s.filterVal === this.props.filter.taskStatus);
    const navItems = TaskFilters.TASK_STATES.map((s, index) => {
      return (
        <OverlayTrigger key={index} placement="top" overlay={<Tooltip id={index}>{s.tip}</Tooltip>} delay={500}>
          <NavItem
            eventKey={index}
            title={s.tip}
            active={index === selectedIndex}
            onClick={() => this.handleStatusSelect(index)}>
              {s.displayVal}
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
        <OverlayTrigger placement="bottom" rootClose={true} trigger="click" overlay={<Tooltip id="glob-reminder">* is a wildcard character</Tooltip>} delay={500}>
          <input
            type="search"
            ref="search"
            className="big-search-box"
            placeholder="Filter tasks"
            value={this.props.filter.filterText}
            onChange={(...args) => this.handleSearchChange(...args)}
            maxLength="128"
          />
        </OverlayTrigger>
        <div className="remove-button" onClick={() => this.clearSearch()}></div>
      </div>
    );
  }

  renderRequestTypeFilter() {
    const filterItems = this.props.displayRequestTypeFilters && TaskFilters.REQUEST_TYPES.map((t, index) => {
      return (
        <li key={index} className={_.contains(this.props.filter.requestTypes, t) ? 'active' : ''}>
          <a onClick={() => this.toggleRequestType(t)}>
            <Glyphicon glyph="ok" /> {Utils.humanizeText(t)}
          </a>
        </li>
      );
    });

    return (
      <div className="requests-filter-container">
        <ul className="nav nav-pills nav-pills-multi-select">
          {filterItems}
          <InfoModalButton title="Filter Tasks" className="inline-button" id="filter-tasks-hint">
            <ul>
              <li>Tasks will be substring-matched by task Id, host and rack.</li>
              <li>Matches at the beginning of the task Id are sorted above everything else.</li>
              <li><strong>You can use <code>*</code> as a wildcard character.</strong></li>
              <li>Use the task state selectors to filter by one task state.</li>
              <li>Use the request type selectors to filter by one or more reqeust types. These are only availible for active tasks.</li>
            </ul>
          </InfoModalButton>
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
