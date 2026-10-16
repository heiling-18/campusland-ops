package com.campuslands.ops.config;

import com.campuslands.ops.model.*;
import com.campuslands.ops.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ImplementoRepository implementoRepository;
    private final ReservaRepository reservaRepository;
    private final PrestamoRepository prestamoRepository;
    private final IncidenciaRepository incidenciaRepository;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           EspacioRepository espacioRepository,
                           ImplementoRepository implementoRepository,
                           ReservaRepository reservaRepository,
                           PrestamoRepository prestamoRepository,
                           IncidenciaRepository incidenciaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.espacioRepository = espacioRepository;
        this.implementoRepository = implementoRepository;
        this.reservaRepository = reservaRepository;
        this.prestamoRepository = prestamoRepository;
        this.incidenciaRepository = incidenciaRepository;
    }

    @Override
    public void run(String... args) {
        java.io.File cleanMarker = new java.io.File("data/.clean_initial_v1.done");
        if (!cleanMarker.exists()) {
            reservaRepository.deleteAll();
            prestamoRepository.deleteAll();
            incidenciaRepository.deleteAll();

            List<Espacio> espaciosExistentes = espacioRepository.findAll();
            for (Espacio esp : espaciosExistentes) {
                esp.setEstado("DISPONIBLE");
                esp.setMotivoBloqueo(null);
                esp.setOrigenBloqueo(null);
            }
            if (!espaciosExistentes.isEmpty()) {
                espacioRepository.saveAll(espaciosExistentes);
            }

            List<Implemento> implementosExistentes = implementoRepository.findAll();
            for (Implemento imp : implementosExistentes) {
                imp.setEstado("DISPONIBLE");
                imp.setPrestadoActualmente(false);
                imp.setMotivoInhabilitado(null);
            }
            if (!implementosExistentes.isEmpty()) {
                implementoRepository.saveAll(implementosExistentes);
            }

            try {
                if (cleanMarker.getParentFile() != null) {
                    cleanMarker.getParentFile().mkdirs();
                }
                cleanMarker.createNewFile();
            } catch (Exception ignored) {
            }
        }

        if (usuarioRepository.count() == 0) {
            cargarUsuariosOficiales();
        }
        if (espacioRepository.count() == 0) {
            cargarEspaciosEInventarioOficial();
        }
    }

    private void cargarUsuariosOficiales() {
        List<Usuario> usuarios = List.of(
            new Usuario("Administrador General", "admin@campuslands.com", "admin123", RolUsuario.ADMIN),
            new Usuario("Trainer Principal", "trainer@campuslands.com", "trainer123", RolUsuario.TRAINER),
            new Usuario("Trainer Secundario", "trainer2@campuslands.com", "trainer123", RolUsuario.TRAINER),
            new Usuario("Soporte Técnico Campus", "soporte@campuslands.com", "soporte123", RolUsuario.SOPORTE),
            new Usuario("Camper Estudiante", "camper@campuslands.com", "camper123", RolUsuario.CAMPER)
        );
        usuarioRepository.saveAll(usuarios);
    }

    private void cargarEspaciosEInventarioOficial() {
        Espacio auditorio = espacioRepository.save(new Espacio(
            "Auditorio", 100, "AUDITORIO", "DISPONIBLE",
            "2 Televisores con 2 Cables HDMI"
        ));
        Espacio spooki = espacioRepository.save(new Espacio(
            "Spooki", 38, "AULA_ENTRENAMIENTO", "DISPONIBLE",
            "35 Computadores completos (Monitor, Mouse, Torre y Teclado)"
        ));
        Espacio apolo = espacioRepository.save(new Espacio(
            "Apolo", 34, "AULA_ENTRENAMIENTO", "DISPONIBLE",
            "34 Computadores completos (Monitor, Mouse, Torre y Teclado)"
        ));
        Espacio artemis = espacioRepository.save(new Espacio(
            "Artemis", 32, "AULA_ENTRENAMIENTO", "DISPONIBLE",
            "32 Computadores completos (Monitor, Mouse, Torre y Teclado)"
        ));
        Espacio cosmos = espacioRepository.save(new Espacio(
            "Cosmos", 25, "AULA_ENTRENAMIENTO", "DISPONIBLE",
            "1 Televisor con su Cable HDMI"
        ));
        Espacio skilab = espacioRepository.save(new Espacio(
            "Skilab", 20, "LABORATORIO", "DISPONIBLE",
            "Kit de Laboratorio Especializado + 1 Pantalla con Cable HDMI"
        ));
        Espacio blanca = espacioRepository.save(new Espacio(
            "Sala Blanca", 12, "SALA_ESTUDIO", "DISPONIBLE",
            "1 Televisor con su Cable HDMI"
        ));
        Espacio azul = espacioRepository.save(new Espacio(
            "Sala Azul", 10, "SALA_ESTUDIO", "DISPONIBLE",
            "1 Televisor con su Cable HDMI"
        ));
        Espacio amarilla = espacioRepository.save(new Espacio(
            "Sala Amarilla", 6, "SALA_ESTUDIO", "DISPONIBLE",
            "1 Televisor con su Cable HDMI"
        ));
        Espacio verde = espacioRepository.save(new Espacio(
            "Sala Verde", 6, "SALA_ESTUDIO", "DISPONIBLE",
            "1 Televisor con su Cable HDMI"
        ));

        List<Implemento> implementos = new ArrayList<>();

        // 1. Auditorio: 2 Televisores con sus Cables HDMI
        implementos.add(new Implemento(auditorio.getId(), auditorio.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-AUD-01", "N/A", "N/A", "HDMI-AUD-01", "Televisor Principal #1 + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(auditorio.getId(), auditorio.getNombre(), "TELEVISOR_HDMI", 2,
            "TV-AUD-02", "N/A", "N/A", "HDMI-AUD-02", "Televisor Secundario #2 + Cable HDMI", "DISPONIBLE", null));

        // 2. Cosmos, Skilab, Blanca, Azul, Amarilla, Verde (Televisor + Cable HDMI)
        implementos.add(new Implemento(cosmos.getId(), cosmos.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-COS-01", "N/A", "N/A", "HDMI-COS-01", "Televisor Cosmos + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(skilab.getId(), skilab.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-SKI-01", "N/A", "N/A", "HDMI-SKI-01", "Pantalla Interactiva Skilab + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(blanca.getId(), blanca.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-BLA-01", "N/A", "N/A", "HDMI-BLA-01", "Televisor Sala Blanca (Nueva) + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(azul.getId(), azul.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-AZU-01", "N/A", "N/A", "HDMI-AZU-01", "Televisor Sala Azul + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(amarilla.getId(), amarilla.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-AMA-01", "N/A", "N/A", "HDMI-AMA-01", "Televisor Sala Amarilla + Cable HDMI", "DISPONIBLE", null));
        implementos.add(new Implemento(verde.getId(), verde.getNombre(), "TELEVISOR_HDMI", 1,
            "TV-VER-01", "N/A", "N/A", "HDMI-VER-01", "Televisor Sala Verde + Cable HDMI", "DISPONIBLE", null));

        // 3. Inventario PDF 1 -> Salon Spooki (35 Estaciones completas operativas)
        String[][] spookiPcs = {
            {"COM2024CM21070374", "C/M 891U-10591", "400SN39180", "KBM 130090708"},
            {"COM2024CM21070711", "S/N 20220501227", "400SN39199", "KBM 130091715"},
            {"COM2024CM21070735", "S/N 20220501406", "102SN55105", "KBM 130090704"},
            {"COM2024CM21070151", "S/N 20220501220", "102SN55097", "KBM 130090717"},
            {"COM2024CM21070127", "S/N 20220501198", "102SN55104", "KBM 130090715"},
            {"COM2024CM21070029", "S/N 20220501419", "102SN55084", "KBM 130090705"},
            {"COM2024CM21070754", "S/N 20220501247", "400SN39194", "KBM 130091705"},
            {"COM2024CM21070135", "S/N 20220501238", "102SN55091", "KBM 130090716"},
            {"COM2024CM21070705", "S/N 20220501349", "400SN39200", "KBM 130091732"},
            {"COM2024CM21070663", "S/N 20220501217", "400SN39187", "KBM 130090714"},
            {"COM2024CM21070188", "S/N 20220501194", "102SN55092", "KBM 130090709"},
            {"COM2024CM21070874", "S/N 20220500310", "400SN39179", "KBM 130090749"},
            {"COM2024CM21070629", "S/N 20220501335", "400SN39224", "KBM 130090748"},
            {"COM2024CM21070287", "S/N 20220501338", "102SN55108", "KBM 1300911748"},
            {"COM2024CM21070018", "S/N 20220501206", "102SN55098", "KBM 130090710"},
            {"COM2024CM21070638", "S/N 20220501204", "400SN39183", "KBM 130091709"},
            {"COM2024CM21070733", "S/N 20220500365", "102SN55081", "KBM 130090731"},
            {"COM2024CM21070001", "S/N 20220501358", "800SN04707", "KBM 130091707"},
            {"COM2024CM21070211", "S/N 20220501197", "400SN39178", "KBM 130091731"},
            {"COM2024CM21070502", "S/N 20220500028", "400SN39229", "KBM 130090733"},
            {"COM2024CM21070683", "CM 891U-13066",   "400SN39173", "KBM 130091719"},
            {"COM2024CM21070880", "S/N 20220500074", "400SN39208", "KBM 130091701"},
            {"COM2024CM21070838", "S/N 20220501417", "400SN39182", "KBM 130090723"},
            {"COM2024CM21070876", "C/M 891U-10586",  "400SN39190", "KBM130090719"},
            {"COM2024CM21070136", "S/N 20220500361", "400SN39186", "UD 18J2C00169"},
            {"COM2024CM21070723", "S/N 20220501229", "400SN39198", "KBM 130090703"},
            {"COM2024CM21070851", "S/N 20220501207", "102SN55107", "CK 440U-10752"},
            {"COM2024CM21070115", "S/N 20220501421", "102SN55106", "KBM 130090718"},
            {"COM2024CM21070050", "S/N 20220501258", "400SN39204", "KBM 130091714"},
            {"COM2024CM21070105", "S/N 20220501336", "400SN39214", "KBM 130090732"},
            {"COM2024CM21070010", "S/N 20220501242", "400SN39189", "KBM 130091733"},
            {"COM2024CM21070109", "S/N 20220501269", "400SN39223", "KBM 130091708"},
            {"COM2024CM21070394", "S/N 20220501360", "400SN39184", "KBM 130091717"},
            {"COM2024CM21070520", "CM 891U-11026",   "102SN55099", "KBM 130090751"},
            {"COM2024CM21070846", "S/N 20220500311", "400SN39237", "KBM 130091751"}
        };
        for (int i = 0; i < spookiPcs.length; i++) {
            implementos.add(new Implemento(spooki.getId(), spooki.getNombre(), "PC_ESTACION", i + 1,
                spookiPcs[i][0], spookiPcs[i][1], spookiPcs[i][2], spookiPcs[i][3],
                "Estación PC Spooki #" + (i + 1), "DISPONIBLE", null));
        }

        // 4. Inventario PDF 2 -> Salon Apolo (34 Estaciones completas operativas)
        String[][] apoloPcs = {
            {"COM2024CM21070117", "S/N 20220501361", "400SN39236", "KBM130091739"},
            {"COM2024CM21070205", "S/N 20220500123", "400SN39216", "KBM130091728"},
            {"COM2024CM21070712", "S/N 20220501235", "400SN39176", "KBM130091713"},
            {"COM2024CM21070853", "S/N 20220501065", "102SN55085", "KBM130091727"},
            {"COM2024CM21070129", "KBM 130090722",   "400SN39175", "S/N 34E97357000881"},
            {"COM2024CM21070609", "S/N 20220500010", "400SN39206", "073221-ML3204-0533"},
            {"COM2024CM21070069", "S/N 20220501159", "400SN39212", "KBM130090734"},
            {"COM2024CM21070778", "S/N 20220501174", "400SN39217", "KBM130091742"},
            {"COM2024CM21070349", "S/N 20220501272", "400SN39203", "KBM130090743"},
            {"COM2024CM21070727", "S/N 20220500086", "400SN39201", "KBM130090742"},
            {"COM2024CM21070063", "S/N 20220501013", "400SN39207", "KBM130090728"},
            {"COM2024CM21070203", "S/N 20220500065", "400SN39193", "KBM130091750"},
            {"COM2024CM21070517", "S/N 20220501208", "400SN39213", "KBM130091710"},
            {"COM2024CM21070008", "S/N 20220500362", "400SN39170", "KBM130090739"},
            {"COM2024CM21070007", "S/N 20220501168", "400SN39197", "KBM130091740"},
            {"COM2024CM21070144", "S/N 20220500316", "102SN55100", "KBM130091734"},
            {"COM2024CM21070771", "S/N 20220501317", "400SN39231", "CK440U-10657"},
            {"COM2024CM21070400", "S/N 2022050137",  "400SN39220", "KBM130091726"},
            {"COM2024CM21070257", "S/N 2022050035",  "400SN39171", "KBM130090727"},
            {"COM2024CM21070526", "S/N 20220501202", "102SN55078", "KBM130090741"},
            {"COM2024CM21070017", "S/N 20220501171", "400SN39232", "KBM130091749"},
            {"COM2024CM21070898", "S/N 20220501271", "400SN39195", "KBM130090740"},
            {"COM2024CM21070534", "S/N 20220501203", "400SN39181", "KBM130090729"},
            {"COM2024CM21070277", "S/N 20220501183", "400SN39192", "KBM130091711"},
            {"COM2024CM21070557", "S/N 20220501208", "102SN55101", "KBM130090713"},
            {"COM2024CM21070406", "S/N 20220501181", "102SN55082", "S/N 34E97357000697"},
            {"COM2024CM21070893", "S/N 20220501424", "400SN39233", "KBM130091741"},
            {"COM2024CM21070850", "S/N 20220501157", "102SN55077", "KBM130090725"},
            {"COM2024CM21070140", "S/N 20220501362", "400SN39211", "KBM130090750"},
            {"COM2024CM21070407", "CM 891U-10656",   "400SN39219", "KBM130091743"},
            {"COM2024CM21070744", "S/N 20220501010", "102SN55093", "KBM130090726"},
            {"COM2024CM21070316", "CM 891U-10657",   "400SN39210", "KBM130091752"},
            {"COM2024CM21070830", "S/N 20220500019", "400SN39168", "KBM130090753"},
            {"COM2024CM21070004", "KBM 130090741",   "400SN39191", "KBM130091753"}
        };
        for (int i = 0; i < apoloPcs.length; i++) {
            implementos.add(new Implemento(apolo.getId(), apolo.getNombre(), "PC_ESTACION", i + 1,
                apoloPcs[i][0], apoloPcs[i][1], apoloPcs[i][2], apoloPcs[i][3],
                "Estación PC Apolo #" + (i + 1), "DISPONIBLE", null));
        }

        // 5. Inventario PDF 3 -> Salon Artemis (32 Estaciones completas operativas)
        String[][] artemisPcs = {
            {"COM2024CM21070834", "S/N 20220501262", "400SN39169", "KBM130090747"},
            {"COM2024CM21070239", "S/N 20220501009", "400SN39227", "KBM130090723"},
            {"COM2024CM21070418", "S/N 20220501392", "400SN39215", "KBM130091746"},
            {"COM2024CM21070574", "S/N 20220501240", "102SN55083", "KBM130091747"},
            {"COM2024CM21070442", "S/N 20220501241", "102SN55086", "KBM130090746"},
            {"COM2024CM21070223", "S/N 20220501012", "400SN39172", "KBM130091745"},
            {"COM2024CM21070236", "S/N 20220501348", "400SN39174", "KBM130090722"},
            {"COM2024CM21070097", "S/N 20220501359", "102SN55095", "KBM130091702"},
            {"COM2024CM21070515", "S/N 20220501339", "102SN55080", "KBM130091724"},
            {"COM2024CM21070498", "S/N 20220501413", "400SN39222", "KBM130091723"},
            {"COM2024CM21070025", "S/N 20220501089", "102SN55090", "KBM130091744"},
            {"COM2024CM21070092", "S/N 20220501209", "400SN39228", "KBM130091736"},
            {"COM2024CM21070318", "S/N 20220500966", "400SN39225", "KBM130090702"},
            {"COM2024CM21070565", "S/N 20220500319", "102SN55094", "KBM130090724"},
            {"COM2024CM21070777", "S/N 20220501376", "102SN55076", "KBM130090721"},
            {"COM2024CM21070759", "S/N 20220501264", "400SN39205", "KBM130090752"},
            {"COM2024CM21070844", "CM 891U-10643",   "102SN55087", "KBM130091722"},
            {"COM2024CM21070503", "S/N 20220501797", "400SN39177", "KBM130091721"},
            {"COM2024CM21070482", "S/N 20220501236", "102SN55102", "KBM130090736"},
            {"COM2024CM21070043", "S/N 20220501796", "400SN39234", "KBM130091738"},
            {"COM2024CM21070786", "S/N 20220500899", "102SN55109", "KBM130090744"},
            {"COM2024CM21070012", "S/N 20220501245", "102SN55110", "KBM130090730"},
            {"COM2024CM21070451", "S/N 20220501365", "400SN39209", "KBM130091720"},
            {"COM2024CM21070138", "S/N 20220500923", "400SN39188", "KBM130091729"},
            {"COM2024CM21070668", "S/N 20220501221", "400SN39235", "KBM130090707"},
            {"COM2024CM21070280", "S/N 20220501228", "400SN39226", "KBM130090706"},
            {"COM2024CM21070464", "S/N 20220501229", "102SN55089", "KBM130090737"},
            {"COM2024CM21070137", "S/N 20220501795", "400SN39202", "KBM130090296"},
            {"COM2024CM21070817", "S/N 20220501274", "102SN55088", "KBM130091737"},
            {"COM2024CM21070504", "S/N 20220501334", "400SN39238", "KBM130091725"},
            {"COM2024CM21070338", "S/N 20220501260", "400SN39230", "KBM130090720"},
            {"COM2024CM21070841", "S/N 20220501263", "102SN55103", "KBM130091730"}
        };
        for (int i = 0; i < artemisPcs.length; i++) {
            implementos.add(new Implemento(artemis.getId(), artemis.getNombre(), "PC_ESTACION", i + 1,
                artemisPcs[i][0], artemisPcs[i][1], artemisPcs[i][2], artemisPcs[i][3],
                "Estación PC Artemis #" + (i + 1), "DISPONIBLE", null));
        }

        // 6. Pool de Equipos para Prestamo Individual de Campers (Modulo 2)
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 1,
            "LAP-CAM-01", "N/A", "LAP-SN-9001", "INTEGRADO", "Portátil Lenovo ThinkPad #01", "DISPONIBLE", null));
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 2,
            "LAP-CAM-02", "N/A", "LAP-SN-9002", "INTEGRADO", "Portátil Lenovo ThinkPad #02", "DISPONIBLE", null));
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 3,
            "LAP-CAM-03", "N/A", "LAP-SN-9003", "INTEGRADO", "Portátil Dell Latitude #03", "DISPONIBLE", null));
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 4,
            "HDMI-CAM-01", "N/A", "N/A", "N/A", "Adaptador USB-C + Cable HDMI #01", "DISPONIBLE", null));
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 5,
            "MOU-CAM-01", "MOU-SN-501", "N/A", "N/A", "Mouse Inalámbrico Logitech #01", "DISPONIBLE", null));
        implementos.add(new Implemento(null, "Bodega Préstamos", "PRESTAMO_CAMPER", 6,
            "KBM-CAM-01", "N/A", "N/A", "KBM-SN-701", "Teclado Mecánico USB #01", "DISPONIBLE", null));

        implementoRepository.saveAll(implementos);
    }
}
