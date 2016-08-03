import React, { PropTypes } from 'react';
import Log from '../src/components/Log';
import httpTailer from '../src/components/httpTailer';

const Debug = (props) => {
  console.log(props.config);
  return <div>{props.path}</div>;
};

const HttpTailer = ({params}) => {
  const path = params.splat;

  const WrappedLog = httpTailer(Log, path);

  return <WrappedLog />;
};

HttpTailer.propTypes = {
  params: PropTypes.object.isRequired
};

export default HttpTailer;
