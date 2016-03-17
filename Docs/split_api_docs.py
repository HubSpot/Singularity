import os
import sys

endpoints = {}
api_index = ['# Singularity REST API', '', '## Endpoints']
models = []
models_index = ['# Models', '']

def endpoint_file(endpoint):
	return endpoint.replace('/api','api').replace('/', '-')

with open(os.path.join(os.path.dirname(__file__),'reference/api.md')) as alldocs:
    current_endpoint = None
    in_models = False
    in_models_index = False
    for line in alldocs:
        line = line.rstrip().replace('#model', 'models.md#model')
        if line.startswith('Models:'):
            in_models_index = True
        if line.startswith('### <a name="endpoint-'):
            current_endpoint = line.split(' ')[-1]
            endpoints[current_endpoint] = []
            api_index.append('- [`{0}`]({1}.md)'.format(current_endpoint, endpoint_file(current_endpoint)))
            continue
        if 'Data Types' in line:
            current_endpoint = None
            in_models = True
        if current_endpoint:
            endpoints[current_endpoint].append(line)
        if in_models:
            models.append(line)
        if in_models_index:
            models_index.append(line)
            if not line.startswith('-') and not line.startswith('Models:'):
                in_models_index = False

api_index.extend(['', '## [Models](models.md)'])

for endpoint, contents in endpoints.iteritems():
    with open(os.path.join(os.path.dirname(__file__), 'reference/apidocs/{0}.md'.format(endpoint_file(endpoint))), 'w') as endpoint_docs:
        endpoint_docs.write('\n'.join(contents))

with open(os.path.join(os.path.dirname(__file__),'reference/apidocs/models.md'), 'w') as models_docs:
    models_docs.write('\n'.join(models_index + ['\n'] + models))

with open(os.path.join(os.path.dirname(__file__),'reference/apidocs/api-index.md'), 'w') as api_index_docs:
	api_index_docs.write('\n'.join(api_index))