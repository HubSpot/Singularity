import { Component } from 'react';

import SwaggerUi, {presets} from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';
import 'swagger-ui/dist/swagger-ui.js.map';

class ApiDocs extends Component {

  componentDidMount() {
    SwaggerUi({
      dom_id: '#swaggerContainer',
      url: `${config.apiRoot}/openapi.json`,
      presets: [presets.apis],
      defaultModelRendering: 'model',
      supportedSubmitMethods: ['get']
    });
  }

  render() {
    return (
      <div id="swaggerContainer" />
    );
  }
}

export default ApiDocs;