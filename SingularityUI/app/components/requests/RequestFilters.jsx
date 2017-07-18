import React from 'react';
import Utils from '../../utils';
import classNames from 'classnames';
import { Link } from 'react-router';

import { Nav, NavItem, Glyphicon, Button } from 'react-bootstrap';

export default class RequestFilters extends React.Component {

  static propTypes = {
    displayRequestTypeFilters: React.PropTypes.bool
  };

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
      displayVal: 'Cooldown'
    },
    {
      filterVal: 'paused',
      displayVal: 'Paused',
      separatorAfter: true
    },
    {
      filterVal: 'pending',
      displayVal: 'Pending'
    },
    {
      filterVal: 'cleaning',
      displayVal: 'Cleaning',
      separatorAfter: true
    },
    {
      filterVal: 'activeDeploy',
      displayVal: 'Active Deploy'
    },
    {
      filterVal: 'noDeploy',
      displayVal: 'No Deploy',
      separatorAfter: true
    },
    {
      filterVal: 'overUtilizedCpu',
      displayVal: 'Over-utilized CPU'
    },
    {
      filterVal: 'underUtilizedCpu',
      displayVal: 'Under-utilized CPU'
    },
    {
      filterVal: 'underUtilizedMem',
      displayVal: 'Under-utilized Memory'
    }
  ];

  static REQUEST_TYPES = ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];

  handleStatusSelect(selectedKey) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {state: RequestFilters.REQUEST_STATES[selectedKey].filterVal}));
  }

  handleSearchChange(event) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {searchFilter: event.target.value}));
  }

  toggleRequestType(requestType) {
    let selected = this.props.filter.subFilter;
    if (selected.length === RequestFilters.REQUEST_TYPES.length) {
      selected = [requestType];
    } else if (_.isEmpty(_.without(selected, requestType))) {
      selected = RequestFilters.REQUEST_TYPES;
    } else if (_.contains(selected, requestType)) {
      selected = _.without(selected, requestType);
    } else {
      selected.push(requestType);
    }
    this.props.onFilterChange(_.extend({}, this.props.filter, {subFilter: selected}));
  }

  clearSearch() {
    this.props.onFilterChange(_.extend({}, this.props.filter, {searchFilter: ''}));
  }

  renderStatusFilter() {
    const selectedIndex = _.findIndex(RequestFilters.REQUEST_STATES, (requestState) => requestState.filterVal === this.props.filter.state);
    const navItems = RequestFilters.REQUEST_STATES.map((requestState, index) => {
      return (
        <NavItem
          key={index}
          className={classNames({'separator-pill': requestState.separatorAfter})}
          eventKey={index}
          title={requestState.tip}
          active={index === selectedIndex}
          onClick={() => this.handleStatusSelect(index)}>
            {requestState.displayVal}
        </NavItem>
      );
    });

    return (
      <Nav bsStyle="pills" className="table-nav-pills" activeKey={selectedIndex}>
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
          placeholder="Filter requests"
          value={this.props.filter.searchFilter}
          onChange={(...args) => this.handleSearchChange(...args)}
          maxLength="128"
        />
        <div className="remove-button" onClick={() => this.clearSearch()}></div>
      </div>
    );
  }

  renderRequestTypeFilter() {
    const filterItems = this.props.displayRequestTypeFilters && RequestFilters.REQUEST_TYPES.map((requestType, index) => {
      return (
        <li key={index} className={_.contains(this.props.filter.subFilter, requestType) ? 'active' : ''}>
          <a onClick={() => this.toggleRequestType(requestType)}>
            <Glyphicon glyph="ok" /> {Utils.humanizeText(requestType)}
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
    const newRequestButton = !config.hideNewRequestButton && (
      <Link to={'requests/new'}>
        <Button bsStyle="success">
          <Glyphicon glyph="plus" /> New Request
        </Button>
      </Link>
    );

    return (
      <div>
        <div className="row">
          <div className="col-md-10">
            {this.renderStatusFilter()}
          </div>
          <div className="col-md-2 text-right">
            {newRequestButton}
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
