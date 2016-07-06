import React from 'react';

import { Navbar, NavItem, Nav, NavDropdown, MenuItem, Glyphicon } from 'react-bootstrap';

function goTo(props, event, route) {
  event.preventDefault();
  console.log(props);
  props.history.push(route);
}

const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  return (
    <Navbar fluid={true}>
      <Navbar.Header>
        <Navbar.Brand>
          <a href={config.appRoot}>{config.title}</a>
        </Navbar.Brand>
        <Navbar.Toggle />
      </Navbar.Header>
      <Navbar.Collapse>
        <Nav>
          <NavItem eventKey={1} active={fragment === ''} onClick={(e) => goTo(props, e, '')}>Dashboard</NavItem>
          <NavItem eventKey={2} active={fragment === 'status'} onClick={(e) => goTo(props, e, 'status')}>Status</NavItem>
          <NavItem eventKey={3} active={_.contains(['requests', 'request'], fragment)} onClick={(e) => goTo(props, e, 'requests')}>Requests</NavItem>
          <NavItem eventKey={4} active={_.contains(['tasks', 'task'], fragment)} onClick={(e) => goTo(props, e, 'tasks')}>Tasks</NavItem>
          <NavDropdown eventKey={5} title="Admin">
            <MenuItem eventKey={5.1} href={`${config.appRoot}/racks`} active={fragment === 'racks'}>Racks</MenuItem>
            <MenuItem eventKey={5.2} href={`${config.appRoot}/slaves`} active={fragment === 'slaves'}>Slaves</MenuItem>
            <MenuItem eventKey={5.3} href={`${config.appRoot}/webhooks`} active={fragment === 'webhooks'}>Webhooks</MenuItem>
            <MenuItem divider={true} />
            <MenuItem eventKey={5.4} href={`${config.appRoot}/taskSearch`}>Task search</MenuItem>
          </NavDropdown>
          <NavItem eventKey={6} target="blank" href={config.apiDocs}>API Docs <small>(Beta)</small></NavItem>
          <NavItem eventKey={7} className="global-search-button">
            <Glyphicon glyph="search" />
            <span className="icon-search-adjacent-text"> Search</span>
          </NavItem>
        </Nav>
      </Navbar.Collapse>
    </Navbar>
  );
};

Navigation.propTypes = {
  location: React.PropTypes.object
};

export default Navigation;
