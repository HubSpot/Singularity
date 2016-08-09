import React, { PropTypes } from 'react';
import { HttpTailer } from '../src/components';

const HttpTailerPage = ({params}) => {
  const path = params.splat;
  return <HttpTailer tailerId={`http|${path}`} path={path} />;
};

HttpTailerPage.propTypes = {
  params: PropTypes.object.isRequired
};

export default HttpTailerPage;
