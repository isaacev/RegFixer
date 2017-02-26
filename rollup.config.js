// rollup.config.js
import typescript from 'rollup-plugin-typescript';

export default {
  entry: './src/app.ts',
  dest: './dist/frontend.js',
  moduleName: 'frontend',
  format: 'iife',

  plugins: [
    typescript()
  ],

  external: [
    'codemirror',
    'codemirror-no-newlines'
  ],

  globals: {
    codemirror: 'CodeMirror'
  }
}
