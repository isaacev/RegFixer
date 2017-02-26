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
    'jquery',
    'codemirror',
    'codemirror-no-newlines'
  ],

  globals: {
    jquery: 'jQuery',
    codemirror: 'CodeMirror'
  }
}
