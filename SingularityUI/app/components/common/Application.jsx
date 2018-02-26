import React from 'react';
import {connect} from 'react-redux';
import Messenger from 'messenger';
import Navigation from './Navigation';
import GlobalSearch from '../globalSearch/GlobalSearch';
import Title from './Title';
import Utils from '../../utils';

const Application = (props) => {
  const taskLagMinutes = props.maxTaskLag / 1000 / 60;
  if (taskLagMinutes >= 3) {
    Messenger().error({
      message: `
        <strong>Singularity is experiencing some delays.</strong>
        The team has already been notified. (Max task lag: ${Utils.duration(props.maxTaskLag)})
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
  children: React.PropTypes.object,
  history: React.PropTypes.object.isRequired,
  location: React.PropTypes.object.isRequired,
  maxTaskLag: React.PropTypes.number,
  params: React.PropTypes.object.isRequired,
  routes: React.PropTypes.object.isRequired,
};

const mapStateToProps = (state) => {
  return {
    maxTaskLag: state.api.status.data.maxTaskLag
  };
};

export default connect(mapStateToProps)(Application);
