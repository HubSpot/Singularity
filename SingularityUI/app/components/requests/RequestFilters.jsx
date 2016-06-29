import React from 'react';
import Utils from '../../utils';

import { Nav, NavItem } from 'react-bootstrap';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class RequestFilters extends React.Component {

  static REQUEST_STATES = [
    {
      filterVal: 'all',
      displayVal: 'All'
    },
    {
      filterVal: 'active',
      displayVal: 'Active'
    },
    {
      filterVal: 'cooldown',
      displayVal: 'Cleaning'
    },
    {
      filterVal: 'paused',
      displayVal: 'Paused'
    },
    {
      filterVal: 'pending',
      displayVal: 'Pending'
    },
    {
      filterVal: 'cleaning',
      displayVal: 'Cleaning'
    },
    {
      filterVal: 'activeDeploy',
      displayVal: 'Active Deploy'
    },
    {
      filterVal: 'noDeploy',
      displayVal: 'No Deploy'
    }
  ];

  static REQUEST_TYPES = ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];

  handleStatusSelect(selectedKey) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {requestStatus: RequestFilters.REQUEST_STATES[selectedKey].filterVal}));
  }

  handleSearchChange(e) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {filterText: e.target.value}));
  }

  toggleRequestType(t) {
    let selected = this.props.filter.subFilter;
    if (selected.length === RequestFilters.REQUEST_TYPES.length) {
      selected = [t];
    } else if (_.isEmpty(_.without(selected, t))) {
      selected = RequestFilters.REQUEST_TYPES;
    } else if (_.contains(selected, t)) {
      selected = _.without(selected, t);
    } else {
      selected.push(t);
    }
    this.props.onFilterChange(_.extend({}, this.props.filter, {requestTypes: selected}));
  }

  renderStatusFilter() {
    const selectedIndex = _.findIndex(RequestFilters.REQUEST_STATES, (s) => s.filterVal === this.props.filter.state);
    const navItems = RequestFilters.REQUEST_STATES.map((s, index) => {
      return (
        <NavItem
          key={index}
          eventKey={index}
          title={s.tip}
          active={index === selectedIndex}
          onClick={() => this.handleStatusSelect(index)}>
            {s.displayVal}
        </NavItem>
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
      <input
        type="search"
        ref="search"
        className="big-search-box"
        placeholder="Filter requests"
        value={this.props.filter.filterText}
        onChange={(...args) => this.handleSearchChange(...args)}
        maxlength="128" />
    );
  }

  renderRequestTypeFilter() {
    const filterItems = this.props.displayRequestTypeFilters && RequestFilters.REQUEST_TYPES.map((t, index) => {
      return (
        <li key={index} className={_.contains(this.props.filter.subFilter, t) ? 'active' : ''}>
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

RequestFilters.propTypes = {
  onFilterChange: React.PropTypes.func.isRequired,
  filter: React.PropTypes.shape({
    state: React.PropTypes.string.isRequired,
    subFilter: React.PropTypes.array.isRequired,
    searchFilter: React.PropTypes.string.isRequired
  }).isRequired
};
