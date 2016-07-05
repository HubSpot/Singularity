import React, { Component } from 'react';

import { Navbar, NavItem, Nav, NavDropdown, MenuItem, Glyphicon } from 'react-bootstrap';

export default class Navigation extends Component {

  render() {
    const fragment = this.props.path.split("/")[0];
    return (
      <Navbar fluid>
        <Navbar.Header>
          <Navbar.Brand>
            <a href={config.appRoot}>{config.title}</a>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Nav>
            <NavItem eventKey={1} href={`${config.appRoot}`} active={fragment === ''}>Dashboard</NavItem>
            <NavItem eventKey={2} href={`${config.appRoot}/status`} active={fragment === 'status'}>Status</NavItem>
            <NavItem eventKey={3} href={`${config.appRoot}/requests`} active={_.contains(['requests', 'request'], fragment)}>Requests</NavItem>
            <NavItem eventKey={4} href={`${config.appRoot}/tasks`} active={_.contains(['tasks', 'task'], fragment)}>Tasks</NavItem>
            <NavDropdown eventKey={5} title="Admin">
              <MenuItem eventKey={5.1} href={`${config.appRoot}/racks`} active={fragment === 'racks'}>Racks</MenuItem>
              <MenuItem eventKey={5.2} href={`${config.appRoot}/slaves`} active={fragment === 'slaves'}>Slaves</MenuItem>
              <MenuItem eventKey={5.3} href={`${config.appRoot}/webhooks`} active={fragment === 'webhooks'}>Webhooks</MenuItem>
              <MenuItem divider />
              <MenuItem eventKey={5.4} href={`${config.appRoot}/taskSearch`}>Task search</MenuItem>
            </NavDropdown>
          </Nav>
          <Nav pullRight>
            <NavItem eventKey={1} target="blank" href={config.apiDocs}>API Docs <small>(Beta)</small></NavItem>
            <NavItem eventKey={2} className="global-search-button">
              <Glyphicon glyph="search" />
              <span className="icon-search-adjacent-text"> Search</span>
            </NavItem>
          </Nav>
        </Navbar.Collapse>
      </Navbar>
    );
  }
}
