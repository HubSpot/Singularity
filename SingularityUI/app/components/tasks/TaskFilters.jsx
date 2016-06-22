import React from 'react';
import Utils from '../../utils';

import { Nav, NavItem, OverlayTrigger, Tooltip } from 'react-bootstrap';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

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
      },
    ];
  }

  static get REQUEST_TYPES() {
    return ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];
  }

  constructor(props) {
    super(props);
    this.state = {
      selectedStatusKey: 0,
      selectedRequestTypes: TaskFilters.REQUEST_TYPES,
      searchValue: ''
    };
  }

  componentWillUpdate(nextProps, nextState) {
    if (nextState != this.state) {
      this.props.onFilterChange({
        taskStatus: TaskFilters.TASK_STATES[nextState.selectedStatusKey].filterVal,
        requestTypes: nextState.selectedRequestTypes,
        filterText: nextState.searchValue
      });
    }
  }

  handleStatusSelect(selectedKey) {
    this.setState({
      selectedStatusKey: selectedKey
    });
  }

  handleSearchChange(e) {
    this.setState({
      searchValue: e.target.value
    });
  }

  toggleRequestType(t) {
    let selected = this.state.selectedRequestTypes;
    console.log(selected, [t]);
    if (selected.length == TaskFilters.REQUEST_TYPES.length) {
      selected = [t];
    } else if (_.isEmpty(_.without(selected, t))) {
      selected = TaskFilters.REQUEST_TYPES;
    } else if (_.contains(selected, t)) {
      selected = _.without(selected, t);
    } else {
      selected.push(t);
    }
    this.setState({
      selectedRequestTypes: selected
    });
  }

  renderStateFilter() {
    const navItems = TaskFilters.TASK_STATES.map((s, index) => {
      return (
        <OverlayTrigger key={index} placement="top" overlay={<Tooltip id={index}>{s.tip}</Tooltip>} delay={500}>
          <NavItem
            eventKey={index}
            title={s.tip}
            active={index == this.state.selectedStatusKey}
            onClick={() => this.handleStatusSelect(index)}>
              {s.displayVal}
          </NavItem>
        </OverlayTrigger>
      );
    });

    return (
      <Nav bsStyle="pills" activeKey={this.state.selectedStatusKey}>
        {navItems}
      </Nav>
    );
  }

  renderSearchInput() {
    return (
      <input
        type="search"
        className="big-search-box"
        placeholder="Filter tasks"
        value={this.state.searchValue}
        onChange={this.handleSearchChange.bind(this)}
        maxlength="128" />
    );
  }

  renderRequestTypeFilter() {
    const filterItems = TaskFilters.REQUEST_TYPES.map((t, index) => {
      return (
        <li key={index} className={!_.contains(this.state.selectedRequestTypes, t) ? 'active' : ''}>
          <a onClick={() => this.toggleRequestType(t)}>
            <Glyphicon iconClass='ok' /> {Utils.humanizeText(t)}
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
            {this.renderStateFilter()}
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
  onFilterChange: React.PropTypes.func.isRequired
};
