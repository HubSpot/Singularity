import React from 'react';
import {connect} from 'react-redux';
import Messenger from 'messenger';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';

const Application = (props) => {
  if (props.maxTaskLag > 3) {
    Messenger().error({
      message: `
        <strong>Singularity is experiencing some delays.</strong> 
        The PaaS Run team has already been notified. (Max task lag: ${props.maxTaskLag} minutes)
      `
    });
  }

  return (
    <div>
      <Title routes={props.routes} params={props.params} />
      <Navigation location={props.location} history={props.history} />
      <GlobalSearch />
      {props.children}
    </div>
  );
};

Application.propTypes = {
  location: React.PropTypes.object.isRequired,
  history: React.PropTypes.object.isRequired,
  children: React.PropTypes.object,
  maxTaskLag: React.PropTypes.number
};

const mapStateToProps = (state) => {
  return {
    maxTaskLag: state.api.status.data.maxTaskLag
  };
};

export default connect(mapStateToProps)(Application);
