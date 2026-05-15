package dam.pmdm.astrobotgincana

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import dam.pmdm.astrobotgincana.databinding.FragmentMapaBinding
import dam.pmdm.astrobotgincana.model.MisionAstroBot

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode

import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher

import android.graphics.BitmapFactory
import org.maplibre.android.annotations.IconFactory
import androidx.core.graphics.scale

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


class MapaFragment : Fragment() {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapLibreMap: MapLibreMap
    private var locationComponentInicializado = false


    // Marcadores del mapa
    private val misiones = listOf(

        MisionAstroBot(
            "Málaga",
            "Recupera los núcleos de energía ocultos en la base principal.",
            36.7213,
            -4.4214,
            "astrobot"
        ),

        MisionAstroBot(
            "Sevilla",
            "Repara los circuitos de la estación Astro Bot.",
            37.3891,
            -5.9845,
            "astrobot"
        ),

        MisionAstroBot(
            "Granada",
            "Protege la fortaleza de los drones enemigos.",
            37.1773,
            -3.5986,
            "astrobot"
        ),

        MisionAstroBot(
            "Córdoba",
            "Encuentra los chips perdidos en la zona antigua.",
            37.8882,
            -4.7794,
            "astrobot"
        ),

        MisionAstroBot(
            "Jaén",
            "Recupera los módulos de navegación dañados.",
            37.7796,
            -3.7849,
            "astrobot"
        ),

        MisionAstroBot(
            "Almería",
            "Desactiva las minas robóticas del desierto.",
            36.8340,
            -2.4637,
            "astrobot"
        ),

        MisionAstroBot(
            "Cádiz",
            "Protege el puerto espacial costero.",
            36.5271,
            -6.2886,
            "astrobot"
        ),

        MisionAstroBot(
            "Huelva",
            "Encuentra las piezas perdidas del reactor.",
            37.2614,
            -6.9447,
            "astrobot"
        ),

        MisionAstroBot(
            "Marbella",
            "Escanea señales alienígenas en la costa.",
            36.5101,
            -4.8825,
            "astrobot"
        ),

        MisionAstroBot(
            "Ronda",
            "Reinicia la torre de comunicaciones.",
            36.7423,
            -5.1671,
            "astrobot"
        )

    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        MapLibre.getInstance(requireContext())

        _binding = FragmentMapaBinding.inflate(inflater, container, false)

        binding.mapView.onCreate(savedInstanceState)

        inicializarMapa()

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun inicializarMapa() {

        binding.mapView.getMapAsync { map ->

            mapLibreMap = map

            val style =
                "https://api.maptiler.com/maps/dataviz-v4/style.json?key=n5tDJ2sXQEllGDVcux6D"

            map.setStyle(
                Style.Builder().fromUri(style)
            ) {

                // marcador personalizado
                val originalBitmap = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.astrobot_marker
                )

                val aspectRatio =
                    originalBitmap.height.toFloat() / originalBitmap.width.toFloat()

                val width = 100
                val height = (width * aspectRatio).toInt()

                val scaledBitmap = originalBitmap.scale(width, height, true)


                val iconoAstroBot = IconFactory.getInstance(requireContext())
                    .fromBitmap(scaledBitmap)


                // Marcadore sobre el mapa de Andaalucía, usaremos los marcadores
                // creados en la variable misiones(están en memoria, pero para el
                // ejemplo sirve
                for (mision in misiones) {

                    val posicion = LatLng(
                        mision.latitud,
                        mision.longitud
                    )

                    map.addMarker(
                        MarkerOptions()
                            .position(posicion)
                            .title(mision.nombre)
                            .snippet("Misión Astro Bot")
                            .icon(iconoAstroBot)
                    )
                }


                // al pulsar sobre un marcador abrimos el dialogo....
                map.setOnMarkerClickListener { marker ->

                    val misionSeleccionada = misiones.find {

                        it.nombre == marker.title
                    }

                    if (misionSeleccionada != null) {

                        mostrarDialogoMision(misionSeleccionada)
                    }

                    true
                }

                //  Mostramos Andalucía
                val andalucia = LatLng(37.5443, -4.7278)

                val cameraPosition = CameraPosition.Builder()
                    .target(andalucia)
                    .zoom(6.5)
                    .build()

                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                )

                inicializarComponenteUbicacion()

                binding.switchUbicacion.setOnCheckedChangeListener { _, isChecked ->

                    if (!locationComponentInicializado) {
                        return@setOnCheckedChangeListener
                    }

                    val locationComponent = mapLibreMap.locationComponent

                    if (isChecked) {

                        locationComponent.isLocationComponentEnabled = true

                        locationComponent.cameraMode = CameraMode.TRACKING

                        locationComponent.renderMode = RenderMode.COMPASS

                    } else {

                        locationComponent.isLocationComponentEnabled = false
                    }
                }

            }
        }
    }

    private fun inicializarComponenteUbicacion() {

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            solicitarPermisosUbicacion()

            return
        }

        val locationComponent = mapLibreMap.locationComponent

        if (!locationComponent.isLocationComponentActivated) {

            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(
                    requireContext(),
                    mapLibreMap.style!!
                ).build()
            )
            locationComponentInicializado = true
        }

        locationComponent.isLocationComponentEnabled = false
    }

    private fun solicitarPermisosUbicacion() {

        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    private fun mostrarDialogoMision(mision: MisionAstroBot) {

        val inputPassword = EditText(requireContext())

        inputPassword.hint = "Introduce la contraseña"

        val layout = LinearLayout(requireContext())

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(50, 40, 50, 10)

        layout.addView(inputPassword)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Misión Astro Bot")

            .setMessage(
                "Localización: ${mision.nombre}\n\n" +
                        "Actividad:\n" +
                        mision.descripcion
            )

            .setView(layout)

            .setPositiveButton("Finalizar misión", null)

            .setNegativeButton("Cancelar", null)

            .create()

        dialog.setOnShowListener {

            val botonFinalizar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            botonFinalizar.isEnabled = false

            inputPassword.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    botonFinalizar.isEnabled =
                        inputPassword.text.toString().isNotEmpty()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

            botonFinalizar.setOnClickListener {

                val password = inputPassword.text.toString()

                if (password == mision.password) {

                    Toast.makeText(
                        requireContext(),
                        "Misión completada 🚀",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Contraseña incorrecta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.mapView.onDestroy()

        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}