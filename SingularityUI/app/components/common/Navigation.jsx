import React, { Component } from 'react';

import { Navbar, NavItem, Nav, NavDropdown, MenuItem, Glyphicon } from 'react-bootstrap';

export default class Navigation extends Component {

  render() {
    return (
      <Navbar>
        <Navbar.Header>
          <Navbar.Brand>
            <a href={config.appRoot}>{config.title}</a>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Nav>
            <NavItem eventKey={1} href={`${config.appRoot}`}>Dashboard</NavItem>
            <NavItem eventKey={2} href={`${config.appRoot}/status`}>Status</NavItem>
            <NavItem eventKey={3} href={`${config.appRoot}/requests`}>Requests</NavItem>
            <NavItem eventKey={4} href={`${config.appRoot}/tasks`}>Tasks</NavItem>
            <NavDropdown eventKey={5} title="Admin">
              <MenuItem eventKey={5.1} href={`${config.appRoot}/racks`}>Racks</MenuItem>
              <MenuItem eventKey={5.2} href={`${config.appRoot}/slaves`}>Slaves</MenuItem>
              <MenuItem eventKey={5.3} href={`${config.appRoot}/webhooks`}>Webhooks</MenuItem>
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
