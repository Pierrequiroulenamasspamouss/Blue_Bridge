function getRoutes(router, base = "") {
  let routes = [];

  router.stack.forEach(layer => {
    if (layer.route && layer.route.path) {
      const path = base + layer.route.path;
      const methods = Object.keys(layer.route.methods).map(m => m.toUpperCase());
      methods.forEach(method => {
        routes.push({ method, path });
      });
    } else if (layer.name === "router" && layer.handle.stack) {
      const mountPath = layer.regexp?.source
        .replace("^\\", "")
        .replace("\\/?(?=\\/|$)", "")
        .replace(/\\\//g, "/")
        .replace(/\$$/, "");

      const nestedBase = base + (mountPath || "");
      routes = routes.concat(getRoutes(layer.handle, nestedBase));
    }
  });

  return routes;
}

function routeExplorerPage(req, res) {
  const routes = getRoutes(req.app._router);
  routes.sort((a, b) => a.path.localeCompare(b.path));

  const html = `
  <!DOCTYPE html>
  <html>
  <head>
    <title>BlueBridge API Routes</title>
    <style>
      body { 
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
        padding: 2rem; 
        background: #f8f9fa; 
        color: #2c3e50;
      }
      .container {
        max-width: 1200px;
        margin: 0 auto;
      }
      h1 { 
        color: #2c3e50; 
        border-bottom: 2px solid #3498db;
        padding-bottom: 0.5rem;
      }
      .route-group {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        margin-bottom: 2rem;
        overflow: hidden;
      }
      .group-header {
        background: #3498db;
        color: white;
        padding: 1rem;
        font-size: 1.2rem;
        font-weight: bold;
      }
      table { 
        width: 100%; 
        border-collapse: collapse;
      }
      th, td { 
        padding: 1rem; 
        text-align: left; 
        border-bottom: 1px solid #eee;
      }
      th { 
        background-color: #f8f9fa;
        font-weight: 600;
      }
      .method { 
        font-weight: bold; 
        padding: 0.4rem 0.8rem; 
        border-radius: 4px; 
        color: white; 
        font-size: 0.8rem; 
        display: inline-block;
        min-width: 80px;
        text-align: center;
      }
      .GET { background-color: #27ae60; }
      .POST { background-color: #2980b9; }
      .PUT { background-color: #f39c12; }
      .DELETE { background-color: #c0392b; }
      .PATCH { background-color: #8e44ad; }
      a { 
        color: #3498db; 
        text-decoration: none; 
        font-family: 'Consolas', monospace;
      }
      a:hover { 
        text-decoration: underline; 
      }
      .timestamp {
        text-align: center;
        color: #7f8c8d;
        margin-top: 2rem;
        font-size: 0.9rem;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <h1>BlueBridge API Routes</h1>
      ${groupRoutesByPrefix(routes)}
      <div class="timestamp">Generated at ${new Date().toLocaleString()}</div>
    </div>
  </body>
  </html>
  `;

  res.setHeader("Content-Type", "text/html");
  res.send(html);
}

function groupRoutesByPrefix(routes) {
  const groups = {};
  
  routes.forEach(route => {
    const prefix = route.path.split('/')[1] || 'root';
    if (!groups[prefix]) {
      groups[prefix] = [];
    }
    groups[prefix].push(route);
  });

  return Object.entries(groups)
    .map(([prefix, routes]) => `
      <div class="route-group">
        <div class="group-header">/${prefix}</div>
        <table>
          <tr><th>Method</th><th>Path</th></tr>
          ${routes.map(({ method, path }) => {
            const displayPath = path.replace(/:([^/]+)/g, '{$1}');
            const link = method === 'GET' ? `<a href="${displayPath}" target="_blank">${displayPath}</a>` : displayPath;
            return `<tr><td><span class="method ${method}">${method}</span></td><td>${link}</td></tr>`;
          }).join('')}
        </table>
      </div>
    `).join('');
}

module.exports = (app) => {
  app.get("/routes", routeExplorerPage);
};