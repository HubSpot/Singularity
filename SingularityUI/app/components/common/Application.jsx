import React from 'react';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';

const Application = (props) => {
  return (<div>
    <Title routes={props.routes} params={props.params} />
    <Navigation location={props.location} history={props.history} />
    <GlobalSearch />
    {props.children}
  </div>);
};

Application.propTypes = {
  location: React.PropTypes.object.isRequired,
  history: React.PropTypes.object.isRequired,
  children: React.PropTypes.object
};

export default Application;
