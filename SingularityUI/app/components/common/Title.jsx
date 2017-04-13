import React from 'react';

const getLastMatchingRouteTitle = (routes, params) => {
  for (var i=routes.length - 1; i >= 0; i--) {
    if (routes[i].hasOwnProperty('title')) {
      if (typeof routes[i].title === 'function') {
        return routes[i].title(params)
      } else {
        return routes[i].title
      }
    }
  }

  return '';
}

const Title = ({routes, params}) => {
  if (typeof document !== 'undefined') {
    const newTitle = getLastMatchingRouteTitle(routes, params) + " - " + config.title;

    if (document.title !== newTitle) {
      document.title = newTitle;
    }
  }

  return null;
};

Title.propTypes = {
  routes: React.PropTypes.array.isRequired,
  params: React.PropTypes.object.isRequired,
}

export default Title; 