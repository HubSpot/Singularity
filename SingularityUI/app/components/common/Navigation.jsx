import React from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import { ToggleVisibility } from '../../actions/ui/globalSearch';
import classnames from 'classnames';
import Utils from '../../utils';

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
      fragment === 'agents' ||
      fragment === 'webhooks' ||
      fragment === 'task-search'
    );
  }
  if (navbarPath === '/') {
    return fragment === '';
  }
  return navbarPath === fragment;
}

function currentPathForLink(currentPath) {
  try {
    // The same task id will likely not exist in another singularity cluster, redirect to request page instead
    if (currentPath.startsWith('/task/')) {
      const requestId = Utils.getTaskDataFromTaskId(currentPath.split('/')[2]).requestId
      return '/request/' + requestId
    } else {
      return currentPath;
    }
  } catch (err) {
    return currentPath;
  }
}

// put into page wrapper, render children
const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  const navTitle = config.navTitleLinks
    ? (
      <ul className="nav navbar-nav">
        <li className="dropdown">
          <a href="#" className="dropdown-toggle navbar-brand" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
            {config.title} <span className="caret" />
          </a>
          <ul className="dropdown-menu">
            {Object.keys(config.navTitleLinks).map((linkTitle, index) =>
              <li key={index}>
                <a href={config.navTitleLinks[linkTitle].replace('{CURRENT_PATH}', currentPathForLink(props.location.pathname))}>{linkTitle}</a>
              </li>
            )}
          </ul>
        </li>
      </ul>
    ) : (
      <Link className="navbar-brand" to="/">{config.title}</Link>
    );

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
          {navTitle}
        </div>
        <div className="collapse navbar-collapse" id="navbar-collapse">
          <ul className="nav navbar-nav">
            <li className={classnames({active: isActive('requests', fragment)})}>
              <Link to="/requests">Requests {isActive('requests', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('status', fragment)})}>
              <Link to="/status">Status {isActive('status', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('tasks', fragment)})}>
              <Link to="/tasks">Tasks {isActive('tasks', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames('dropdown', {active: isActive('admin', fragment)})}>
              <a href="#" className="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                Admin <span className="caret" />
              </a>
              <ul className="dropdown-menu">
                <li><Link to="/racks">Racks</Link></li>
                <li><Link to="/agents">Agents</Link></li>
                <li><Link to="/utilization">Utilization</Link></li>
                <li><Link to="/webhooks">Webhooks</Link></li>
                <li><Link to="/disasters">Disasters</Link></li>
                <li role="separator" className="divider"></li>
                <li><Link to="/task-search">Task Search</Link></li>
              </ul>
            </li>
            <li><Link to="/api-docs">API Docs</Link></li>
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
