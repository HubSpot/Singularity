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
          <NavItem eventKey={1} active={fragment === ''} onClick={(event) => goTo(props.router, event, '')}>Dashboard</NavItem>
          <NavItem eventKey={2} active={fragment === 'status'} onClick={(event) => goTo(props.router, event, 'status')}>Status</NavItem>
          <NavItem eventKey={3} active={_.contains(['requests', 'request'], fragment)} onClick={(event) => goTo(props.router, event, 'requests')}>Requests</NavItem>
          <NavItem eventKey={4} active={_.contains(['tasks', 'task'], fragment)} onClick={(event) => goTo(props.router, event, 'tasks')}>Tasks</NavItem>
          <NavDropdown id="admin-dropdown" eventKey={5} active={_.contains(['racks', 'slaves', 'webhooks', 'disabled-actions'], fragment)} title="Admin">
            <MenuItem eventKey={5.1} onClick={(event) => goTo(props.router, event, 'racks')}>Racks</MenuItem>
            <MenuItem eventKey={5.2} onClick={(event) => goTo(props.router, event, 'slaves')}>Slaves</MenuItem>
            <MenuItem eventKey={5.3} onClick={(event) => goTo(props.router, event, 'webhooks')}>Webhooks</MenuItem>
            <MenuItem eventKey={5.4} onClick={(event) => goTo(props.router, event, 'disabled-actions')}>Disabled Actions</MenuItem>
            <MenuItem divider={true} />
            <MenuItem eventKey={5.5} onClick={(event) => goTo(props.router, event, 'task-search')}>Task search</MenuItem>
          </NavDropdown>
          <NavItem eventKey={6} target="blank" href={config.apiDocs}>API Docs <small>(Beta)</small></NavItem>
          <NavItem eventKey={7} className="global-search-button" onClick={(event) => handleSearchClick(event, props.toggleGlobalSearch)}>
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
