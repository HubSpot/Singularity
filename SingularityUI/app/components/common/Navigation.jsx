import React from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import { ToggleVisibility } from '../../actions/ui/globalSearch';

import { Navbar, NavItem, Nav, NavDropdown, MenuItem, Glyphicon } from 'react-bootstrap';

function goTo(router, event, route) {
  event.preventDefault();
  router.push(route);
}

function handleSearchClick(event, toggleGlobalSearch) {
  event.preventDefault();
  toggleGlobalSearch();
}

// put into page wrapper, render children
const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  return (
    <Navbar fluid={true}>
      <Navbar.Header>
        <Navbar.Brand>
          <Link to="/">{config.title}</Link>
        </Navbar.Brand>
        <Navbar.Toggle />
      </Navbar.Header>
      <Navbar.Collapse>
        <Nav>
          <NavItem eventKey={1} active={fragment === ''} onClick={(e) => goTo(props.router, e, '')}>Dashboard</NavItem>
          <NavItem eventKey={2} active={fragment === 'status'} onClick={(e) => goTo(props.router, e, 'status')}>Status</NavItem>
          <NavItem eventKey={3} active={_.contains(['requests', 'request'], fragment)} onClick={(e) => goTo(props.router, e, 'requests')}>Requests</NavItem>
          <NavItem eventKey={4} active={_.contains(['tasks', 'task'], fragment)} onClick={(e) => goTo(props.router, e, 'tasks')}>Tasks</NavItem>
          <NavDropdown id="admin-dropdown" eventKey={5} active={_.contains(['racks', 'slaves', 'webhooks'], fragment)} title="Admin">
            <MenuItem eventKey={5.1} onClick={(e) => goTo(props.router, e, 'racks')}>Racks</MenuItem>
            <MenuItem eventKey={5.2} onClick={(e) => goTo(props.router, e, 'slaves')}>Slaves</MenuItem>
            <MenuItem eventKey={5.3} onClick={(e) => goTo(props.router, e, 'webhooks')}>Webhooks</MenuItem>
            <MenuItem divider={true} />
            <MenuItem eventKey={5.4} onClick={(e) => goTo(props.router, e, 'task-search')}>Task search</MenuItem>
          </NavDropdown>
          <NavItem eventKey={6} target="blank" href={config.apiDocs}>API Docs <small>(Beta)</small></NavItem>
          <NavItem eventKey={7} className="global-search-button" onClick={(e) => handleSearchClick(e, props.toggleGlobalSearch)}>
            <Glyphicon glyph="search" />
            <span className="icon-search-adjacent-text"> Search</span>
          </NavItem>
        </Nav>
      </Navbar.Collapse>
    </Navbar>
  );
};

Navigation.propTypes = {
  location: React.PropTypes.object.isRequired,
  router: React.PropTypes.object.isRequired,
  toggleGlobalSearch: React.PropTypes.func
};

function mapDispatchToProps(dispatch) {
  return {
    toggleGlobalSearch: () => dispatch(ToggleVisibility())
  };
}

export default connect(null, mapDispatchToProps)(withRouter(Navigation));
