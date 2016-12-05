import React, { PropTypes } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import LineRenderGroup from './LineRenderGroup';

class DangerousLineRenderGroup extends LineRenderGroup {
  render() {
    const lineGroupHTML = renderToStaticMarkup(super.render());

    return (
      <div
        dangerouslySetInnerHTML={{__html: lineGroupHTML}}
      />
    );
  }
}

export default DangerousLineRenderGroup;
