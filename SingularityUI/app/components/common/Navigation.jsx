import React from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import { ToggleVisibility } from '../../actions/ui/globalSearch';
import classnames from 'classnames';

import { Glyphicon } from 'react-bootstrap';

function handleSearchClick(event, toggleGlobalSearch) {
  event.preventDefault();
  toggleGlobalSearch();
}

function isActive(navbarPath, fragment) {
  if (navbarPath === 'requests' || navbarPath === 'request') {
    return _.contains(['requests', 'request'], fragment);
  }
  if (navbarPath === 'tasks' || navbarPath === 'task') {
    return _.contains(['tasks', 'task'], fragment);
  }
  if (navbarPath === 'admin') {
    return (
      fragment === 'racks' ||
      fragment === 'slaves' ||
      fragment === 'webhooks' ||
      fragment === 'task-search'
    );
  }
  if (navbarPath === '/') {
    return fragment === '';
  }
  return navbarPath === fragment;
}

// put into page wrapper, render children
const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  return (
    <nav className="navbar navbar-default">
      <div className="container-fluid">
        <div className="navbar-header">
          <button type="button" className="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse" aria-expanded="false">
            <span className="sr-only">Toggle navigation</span>
            <span className="icon-bar"></span>
            <span className="icon-bar"></span>
            <span className="icon-bar"></span>
          </button>
          <Link className="navbar-brand" to="/">{config.title}</Link>
        </div>
        <div className="collapse navbar-collapse" id="navbar-collapse">
          <ul className="nav navbar-nav">
            <li className={classnames({active: isActive('/', fragment)})}>
              <Link to="/">Dashboard {isActive('/', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('status', fragment)})}>
              <Link to="/status">Status {isActive('status', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('requests', fragment)})}>
              <Link to="/requests">Requests {isActive('requests', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('tasks', fragment)})}>
              <Link to="/tasks">Tasks {isActive('tasks', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames('dropdown', {active: isActive('admin', fragment)})}>
              <a href="#" className="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                Admin <span className="caret"></span>
              </a>
              <ul className="dropdown-menu">
                <li><Link to="/racks">Racks</Link></li>
                <li><Link to="/slaves">Slaves</Link></li>
                <li><Link to="/utilization">Utilization</Link></li>
                <li><Link to="/webhooks">Webhooks</Link></li>
                <li><Link to="/disasters">Disasters</Link></li>
                <li role="separator" className="divider"></li>
                <li><Link to="/task-search">Task Search</Link></li>
              </ul>
            </li>
            <li><a href={config.apiDocs}>API Docs <small>(Beta)</small></a></li>
            <li>
              <a href="#" onClick={(event) => handleSearchClick(event, props.toggleGlobalSearch)}>
                <Glyphicon glyph="search" />
                <span className="icon-search-adjacent-text"> Search</span>
              </a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
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
