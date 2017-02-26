const gulp        = require('gulp')
const rollup      = require('rollup-stream')
const typescript  = require('rollup-plugin-typescript')
const source      = require('vinyl-source-stream')
const concat      = require('gulp-concat')
const compressJS  = require('gulp-uglify')
const del         = require('del')

// Compile Typescript files to ES6 and combine modules with Rollup.
gulp.task('compile:js', () => {
  return rollup({
      entry: './src/app.ts',
      moduleName: 'frontend',
      format: 'iife',
      plugins: [
        typescript()
      ],
      external: [
        'codemirror',
        'codemirror-no-newlines',
        'localforage'
      ],
      globals: {
        codemirror: 'CodeMirror',
        localforage: 'localforage'
      }
    })
    .pipe(source('app.js'))
    .pipe(gulp.dest('./dist'))
})

// Concatenate JS libraries into a single file.
gulp.task('bundle:js', () => {
  // Paths to libraries used by the frontend. Each file in this list is
  // concatenated into ./dist/libs.js and will be concatenated in this order:
  const libs = [
    './node_modules/codemirror/lib/codemirror.js',
    './node_modules/codemirror/addon/display/placeholder.js',
    './node_modules/codemirror-no-newlines/no-newlines.js',
    './node_modules/localforage/dist/localforage.nopromises.min.js'
  ]

  return gulp.src(libs)
    .pipe(concat('libs.js'))
    .pipe(gulp.dest('./dist'))
})

// Uglify app logic and concatenated libraries.
gulp.task('compress:js', ['compile:js', 'bundle:js'], () => {
  return gulp.src('./dist/*.js')
    .pipe(compressJS())
    .pipe(gulp.dest('./dist'))
})

gulp.task('watch:js', () => {
  gulp.watch('src/*.ts', ['compile:js'])
})

// Delete everything inside the ./dist directory including the directory itself.
gulp.task('clean', () => {
  return del([ './dist' ])
})
