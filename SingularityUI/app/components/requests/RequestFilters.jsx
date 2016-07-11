import React from 'react';
import Utils from '../../utils';
import classNames from 'classnames';
import { Link } from 'react-router';

import { Nav, NavItem, Glyphicon, Button } from 'react-bootstrap';

export default class RequestFilters extends React.Component {

  static propTypes = {
    displayRequestTypeFilters: React.PropTypes.bool
  }

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
      displayVal: 'Paused'
    },
    {
      filterVal: 'pending',
      displayVal: 'Pending'
    },
    {
      filterVal: 'cleanup',
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
    this.props.onFilterChange(_.extend({}, this.props.filter, {state: RequestFilters.REQUEST_STATES[selectedKey].filterVal}));
  }

  handleSearchChange(e) {
    this.props.onFilterChange(_.extend({}, this.props.filter, {searchFilter: e.target.value}));
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
    this.props.onFilterChange(_.extend({}, this.props.filter, {subFilter: selected}));
  }

  clearSearch() {
    this.props.onFilterChange(_.extend({}, this.props.filter, {searchFilter: ''}));
  }

  renderStatusFilter() {
    const selectedIndex = _.findIndex(RequestFilters.REQUEST_STATES, (s) => s.filterVal === this.props.filter.state);
    const navItems = RequestFilters.REQUEST_STATES.map((s, index) => {
      return (
        <NavItem
          key={index}
          className={classNames({'separator-pill': _.contains([3, 5], index)})}
          eventKey={index}
          title={s.tip}
          active={index === selectedIndex}
          onClick={() => this.handleStatusSelect(index)}>
            {s.displayVal}
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
    const filterItems = this.props.displayRequestTypeFilters && RequestFilters.REQUEST_TYPES.map((t, index) => {
      return (
        <li key={index} className={_.contains(this.props.filter.subFilter, t) ? 'active' : ''}>
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
        </ul>
      </div>
    );
  }

  render() {
    return (
      <div>
        <div className="row">
          <div className="col-md-10">
            {this.renderStatusFilter()}
          </div>
          <div className="col-md-2 text-right">
            <Link to={'requests/new'}>
              <Button bsStyle="success">
                <Glyphicon glyph="plus" /> New Request
              </Button>
            </Link>
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
